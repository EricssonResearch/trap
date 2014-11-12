Trap Quickstart (Java)
====

In general, Trap attempts to expose an asynchronous socket API. The basic operations (and the ones covered in this document) are:

- *Listen* for incoming connections
- *Open* an outgoing connection
- *Accept* an incoming connection
- *Send* data
- *Receive* data

This document will exemplify how to do all of the above.

# Installation

Add Trap to your project either as a fully-blown, self-contained archive, or piecemeal as Maven Artifacts. Trap depends on slf4j for its logging; if embedded, slf4j will be included.

## Single Dependency Model

Trap provides a basic package that includes four transports in a convenient manner. 