
// Array of classes for Trap Transports

/**
 * Array of classes for Trap transports.
 */
Trap.Transports = {};

Trap.supportsBinary = typeof(Uint8Array) != "undefined";

Trap.Constants = {};
Trap.Constants.OPTION_MAX_CHUNK_SIZE = "trap.maxchunksize";
Trap.Constants.OPTION_ENABLE_COMPRESSION = "trap.enablecompression";
Trap.Constants.OPTION_AUTO_HOSTNAME = "trap.auto_hostname";
Trap.Constants.CONNECTION_TOKEN = "trap.connection-token";
Trap.Constants.TRANSPORT_ENABLED_DEFAULT = true;
Trap.Constants.ENDPOINT_ID = "trap.endpoint-id";
Trap.Constants.ENDPOINT_ID_UNDEFINED = "UNDEFINED";
Trap.Constants.ENDPOINT_ID_CLIENT = "NEW";