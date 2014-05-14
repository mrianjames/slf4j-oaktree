package com.oaktree.core.logging;

import com.oaktree.core.logging.formatters.ConsoleFormatter;
import com.oaktree.core.logging.formatters.IFormatter;
import com.oaktree.core.logging.handlers.ConsoleHandler;
import com.oaktree.core.logging.handlers.ILoggingHandler;

import org.slf4j.ILoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A manager of loggers that is specifically built for low latency applications.
 * Makes low latency loggers on demand if they are not already configured. This framework
 * offers the same functionality as standard java util logging but provides easy to implement
 * interfaces allowing non-locking handler and formatter classes. 
 * 
 * The manager has one public method - getLogger(String) which will give an existing logger from
 * the log store or make one. No synchronization is present in this process outside of the
 * ConcurrentHashMap which stores the loggers.
 * 
 * The logging framework is initialised from system properties as with java.util.logging. 
 * -Doaktree.logging.config.file=properties/logging.properties
 * 
 * A default handler (console) is provided when no properties and loggers are initialised with the
 * root logger level (INFO).
 * 
 * @author OakTreeDesignsLtd
 *
 */
public class LowLatencyLogManager implements ILoggerFactory, LowLatencyLogManagerMBean  {
	
	public static final String LOGGING_FILE = "oaktree.logging.config.file";
	private static ConsoleHandler defaultHandler = new ConsoleHandler(new ConsoleFormatter());
	private static LowLatencyLogger rootLogger = null;
	/**
	 * Our map of loggers keyed on the logger name, normally the class name.
	 */
	private static ConcurrentHashMap<String, LowLatencyLogger> loggers = new ConcurrentHashMap<String, LowLatencyLogger>(100);
	private static Map<String,ILoggingHandler> handlers = new HashMap<String,ILoggingHandler>();
	
	private static boolean recordThread = true;
	private static Properties properties;

	
	static {
		/*
		 * read any setup information from the system properties.
		 */		
		initialiseFromProperies(true);		
	}
	
	//unit tests only
	static void clear() {
		stop();
		handlers.clear();
		loggers.clear();
		rootLogger = null;
		//defaultHandler = null;
	}
	
	public LowLatencyLogManager() {
		registerJMX();
	}


	/**
	 * Gets a previously configured logger, or makes one if not already created.
	 * @param name
	 * @return logger
	 */
	public LowLatencyLogger getLogger(String name) {
		LowLatencyLogger logger = loggers.get(name);
		if (logger == null) {
			logger = createLogger(name);
			LowLatencyLogger l = loggers.putIfAbsent(name, logger);
			if (l != null) {
				logger = l;
			}
		}
		return logger;
	}

	/**
	 * Register as a jmx bean.
	 */
	private void registerJMX() {
		MBeanServer mbs =ManagementFactory.getPlatformMBeanServer();
		ObjectName name;
		try {
			name = new ObjectName("Logging" + ":type="
					+ "Oaktree" + ",name=" + "LogManager");
			mbs.registerMBean(this, name);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Make a named logger. Will take parent loggers features. If no explicit parent 
	 * then it will take the root loggers features.
	 * This can get called multiple times by diff threads; the caller of this is responsible for registering
	 * in thread safe manor. Multiple loggers isn't end of world, better than blocking.
	 * @param name
	 * @return
	 */
	private static LowLatencyLogger createLogger(String name) {
		System.out.println("Creating logger " + name);
		LowLatencyLogger logger = new LowLatencyLogger(name, recordThread,defaultHandler);
		logger.setHandlers(getHandlersForLogger(name));
		
		/*
		 * set the initial logging level from possible parent.
		 */
		LowLatencyLogger parent = getParentLogger(name);
		if (parent != null) {
			logger.setLevel(parent.getLevel());
		}
		return logger;
	}
	
	private static ILoggingHandler[] getHandlersForLogger(String name) {
		if (properties == null) {
			return new ILoggingHandler[]{defaultHandler};
		}
		String myHandlers = properties != null ? properties.getProperty(name+".handlers") : null;
		if (myHandlers != null) {
			String[] hs = myHandlers.trim().split("[,]");
			if (hs.length > 0) {
				ILoggingHandler[] bmyh = new ILoggingHandler[hs.length];
				int i = 0;
				for (String mh:hs) {
					bmyh[i] = getHandler(mh.trim());
					i++;
				}
				return bmyh;
			}
		}
		LowLatencyLogger parent = getParentLogger(name);
		if (parent != null) {
			ILoggingHandler[] phandlers = parent.getHandlers();
			if (phandlers == null || phandlers.length == 0) {
				//maybe we havent resolved handlers yet...
				resolveHandlers(parent);
				phandlers = parent.getHandlers();
				
			} 
			return phandlers;
		} 
		return handlers.values().toArray(new ILoggingHandler[handlers.size()]);
	}


	/**
	 * For a logger name, walk up the stack of names to find possible loggers
	 * that may exist. Ultimately there should always be one "root" logger.
	 * @param name
	 * @return
	 */
	private static LowLatencyLogger getParentLogger(String name) {
		String[] parentLoggerNames = getParentLoggerNames(name);
		/*
		 * walk backwards through the names, looking for loggers that exist in our collection.
		 */
		LowLatencyLogger logger = null;
		for (int i = parentLoggerNames.length-1; i > 0; i--) {
			logger = loggers.get(parentLoggerNames[i]);
			if (logger != null) {
				return logger;
			}
		}
		/*
		 * found nothing so return the default parent logger - the root logger.
		 */
		return rootLogger;
	}
 
	/**
	 * Get the names of potential parent loggers for a name. Name hierarchies are determined
	 * by a "." character e.g. com.oaktree is the parent of com.oaktree.core, as is com
	 * @param name
	 * @return
	 */
	private static String[] getParentLoggerNames(String name) {
		String[] bits = name.split("[.]");
		List<String> possibles = new ArrayList<String>();
		StringBuilder con = new StringBuilder("");
		for (String bit:bits) {
			con.append(bit);
			possibles.add(con.toString());
			con.append(Text.PERIOD);
		}
		return possibles.toArray(new String[possibles.size()]);
	}

	private final static String recordThreadString = "recordThread";
	
	private static ILoggingHandler makeHandler(Properties p,Set<Object> keys,Set<Object> toRemove,String han) {
		ILoggingHandler handler = null;
		try {
			String clazz = p.getProperty(han+".handler");
			System.out.println("Creating handler " + han + " from class " + clazz);
			handler = (ILoggingHandler) Class.forName(clazz.trim()).newInstance();
			handler.setName(han);
			String formatter = p.getProperty(han + ".formatter");
			String level = p.getProperty(han + ".level");	
			toRemove.add(han+".handler");
			toRemove.add(han + ".formatter");
			toRemove.add(han + ".level");
			if (formatter != null) {
				formatter = formatter.trim();
				System.out.println("Creating formatter " + formatter + " for handler " + han);
				IFormatter f = (IFormatter)Class.forName(formatter).newInstance();
				if (f != null) {
					handler.setFormatter(f);
				} else {
					System.err.println("Could not create formatter "+formatter + " for handler " + han);
				}
			}
			if (level != null) {
				Level l = Level.parse(level.trim());
	            if (l == null) {
	                throw new IllegalArgumentException("Invalid logging level supplied: " + level.trim());
	            }
	            System.out.println("Handler "+han+"level set to " + l.name());
				handler.setLevel(l);
			}
			/*
			 * other properties can be set via reflection....e.g. loggerhandler.filename=c:\\myfile.txt
			 */
			System.out.println("SLF4J Created handler " + handler.getName() + " "+ handler.getClass().getName());
			Method[] methods = handler.getClass().getMethods();
			for (Method method:methods) {
				if (method.getName().startsWith("set")) {
					if (!method.getName().equals("setFormatter") && !method.getName().equals("setLevel") && !method.getName().equals("setHandler")) {
						String field = method.getName().substring(3);
						//lowercase the first char
						char lc = Character.toLowerCase(field.charAt(0));
						field = lc+field.substring(1);
						String value = p.getProperty(han + "."  + field);								
						if (value != null) {
							try {
								toRemove.add(han+"." + field);
								method.setAccessible(true);
								method.invoke(handler, value);
								System.out.println("Set " + value + " for "+han+"."+field);
							} catch (Throwable e) {
								System.err.println("Cannot process field " + field + " value " + value);
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			/*
			 * start the handler
			 */
			handler.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return handler;
	}
	
	/**
	 * Read any system properties and initialise our handlers, formatters, create any initial loggers with the
	 * correct logging levels.
	 */
	public synchronized static void initialiseFromProperies(boolean clear) {
		if (clear) {
			clear();
		}
		System.out.println("****************************************************");
		System.out.println("OaktreeLogging: " + new java.io.File(LowLatencyLogManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName());
		System.out.println("****************************************************");
		String file = System.getProperty(LOGGING_FILE);
		if (file != null) {
            System.out.println("SLF4J Initialising logging from " + file);

            properties = new Properties();
            FileInputStream fis = null;
			try {
				File f = new File(file);
				String fullpath = f.getAbsolutePath();
				System.out.println("SLF4J File: " + fullpath);
				fis = new FileInputStream(f);
				properties.load(fis);				
			} catch (Exception e) {
				System.err.println("SLF4J Invalid file specified for logging: " + e.getMessage());				
				return;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
			if (properties.isEmpty()) {
				System.err.println("Empty file specified for logging.");
				return;
				//throw new IllegalStateException("Empty properties file provided.");
			}
			//TODO change to pick up handlers differently
			Set<Object> keys = properties.keySet();
			Set<Object> toRemove = new HashSet<Object>();

			//HANDLER AND FORMATTER SETUP.
			String hndlrs = properties.getProperty("handlers");
			if (hndlrs != null && hndlrs.length() > 0) {
				toRemove.add("handlers");
				String[] hs = hndlrs.split(",");
				for (String handler:hs) {
					ILoggingHandler hr = makeHandler(properties,keys,toRemove,handler);
					handlers.put(handler,hr);
				}
			} else {
				//root will get a defaultHandler so dont worry. 
			}
			
			keys.removeAll(toRemove);
			String recordT = properties.getProperty(recordThreadString);
			if (recordT != null) {
				recordThread = Boolean.valueOf(recordT);
				keys.remove(recordThreadString);
			}
			
			//ROOT LOGGER SETUP.
			rootLogger = new LowLatencyLogger("root", recordThread,defaultHandler);
			String rootHandlers = properties.getProperty(".handlers");
			if (rootHandlers == null || rootHandlers.length() == 0) {
				if (handlers.size() > 0) {
					rootLogger.setHandlers(handlers.values().toArray(new ILoggingHandler[handlers.size()]));
				} else { //already has default 					
				}
			} else {
				rootLogger.clearHandlers();
				String[] hs = rootHandlers.split("[,]");
				for (String h:hs) {
					rootLogger.addHandler(handlers.get(h));
				}
			}
			String rootLevel = properties.getProperty(".level");
			if (rootLevel == null || rootLevel.length() == 0) {
				rootLogger.setLevel(Level.INFO);
			} else {
				rootLogger.setLevel(Level.parse(rootLevel));
			}
			
			
			
			/*
			 * make any loggers from remaining "levels"
			 */
			for (Object key: keys) {
				String skey = (String)(key);
				if (skey.endsWith(".level")) {
					String x = ((String)key).trim();
					String k = x.replace(".level", "");
					if ("".equals(k)) {
						//already done root
					} else {
						LowLatencyLogger logger = loggers.get(k);
						if (logger == null) {
								logger = createLogger(k);
								loggers.putIfAbsent(k, logger);
						}
						String level = properties.getProperty((String)key);				
						logger.setLevel(Level.parse(level));
					}
				} else if (skey.endsWith(".handlers")) {
					String k = skey.replace(".handlers", "");
					if ("".equals(k)) {
						//root already created.
					} else {
						LowLatencyLogger logger = loggers.get(k);
						if (logger == null) {
							logger = createLogger(k);
							loggers.putIfAbsent(k, logger);
						}
						resolveHandlers(logger);
					} 
					
				} else {
                    System.err.println("Key " + key + " does not contain .level at end - intentional?");
                }
			}
		} else {
			/*
			 * create default setup - console handler & formatter.
			 */
			rootLogger = new LowLatencyLogger("root", recordThread,defaultHandler);
			rootLogger.setLevel(Level.INFO);
						
			rootLogger.info("No configuration[-D"+LOGGING_FILE+"=<yourfile>] supplied on cp; default handler and level initialise");
		}
	}
		
	private static void resolveHandlers(LowLatencyLogger logger) {
		String key = logger.getName() +".handlers";
		String handlers = properties.getProperty((String)key);
		if (handlers == null) {
			return;
		}
		String[] bits = handlers.split("[,]");
		logger.clearHandlers();
		for (String has:bits) {
			ILoggingHandler handler = LowLatencyLogManager.handlers.get(has);
			if (handler != null) {
				logger.addHandler(handler);
			}
		}
	}

	public static ILoggingHandler getHandler(String name) {
		return handlers.get(name);
	}
	
	public static void stop() {
		for (ILoggingHandler handler:handlers.values()) {
			handler.stop();
		}
	}


	@Override
	public boolean doesLoggerExist(String logger) {
		return this.loggers.containsKey(logger);
	}


	@Override
	public int getLoggerLevel(String logger) {
		LowLatencyLogger l = loggers.get(logger);
		int lev = 0;
		if (l != null) {
			lev = l.getLevel();
		}
		return lev;
	}


	@Override
	public void setLoggerLevel(String logger, int level) {
		LowLatencyLogger l = loggers.get(logger);
		if (l != null) {
			l.setLevel(level);
		}
	}


	@Override
	public void setLoggerLevel(String logger, String level) {
		LowLatencyLogger l = loggers.get(logger);
		if (l != null) {
			l.setLevel(Level.parse(level));
		}
	}
}

	