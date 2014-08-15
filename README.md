slf4j-oaktree
=============

A simple, low latency, low contention logging api for slf4j. This library has been developed to solve various performance problems in highly concurrent applications. 

The standard use case it is good for:

I want a logger that is fast, is lock-free on critical paths, allows me to define multiple loggers with background ,foreground file and console loggers that records both thread id and name for easy system debugging even when using background handlers.

Key features:
* Simple: General purpose logic like being able to add appenders at runtime is sacrificed for performance and no locks. Most basic features you would expect off a logger are available.
* Helpful: We include both thread id and name on each log record generated when the logging line was created.
* Fast: No locks on the critical paths.
* Efficient: minimal memory footprint. 

