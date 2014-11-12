Trap Delegate Model
====

Trap is built upon asynchronous operation as a key principle. To use that principle in Java, we employ a delegate model, where callbacks are registered based on specific interests. All of the registrations are done through the _setDelegate()_ method of objects.

An object is a delegate by implementing an interface from the `com.ericsson.research.trap.delegates` package, other than the `TrapDelegate` interface. Each interface in that package contains only one method, representing the method that will be called for the appropriate event.

# Setting a delegate

Set a delegate by invoking the `setDelegate` method. The delegate will be retained by the endpoint.

        this.client.setDelegate(new OnError() {
            
            public void trapError(TrapEndpoint endpoint, Object context)
            {
                System.err.println("Error occurred!");
            }
        }, false);
        
A delegate is retained based on its _interface_. This means that multiple objects can be delegates of the same TrapEndpoint, assuming they implement disparate delegate interfaces. This means that, once the above code is executed, the following code can also be run:

        this.client.setDelegate(new OnData() {
            
            public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
            {
                System.err.println("Data Received!");
            }
        }, false);
        
Both of these delegates will be retained and called as appropriately, as they implement different interfaces. Existing delegates can be cleared by having the second parameter of TrapDelegate be `true`. 

A delegate can implement one or more delegate interfaces. It is possible to implement all delegate methods needed in the same object, or to have dedicated handlers for each and every one.
