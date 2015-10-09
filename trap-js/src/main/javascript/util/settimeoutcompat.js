/**
Workaround for iOS 6 setTimeout bug using requestAnimationFrame to simulate timers during Touch/Gesture-based events
Author: Jack Pattishall (jpattishall@gmail.com)
This code is free to use anywhere (MIT, etc.)
 
Usage: Pass TRUE as the final argument for setTimeout or setInterval.
 
Ex:
setTimeout(func, 1000) // uses native code
setTimeout(func, 1000, true) // uses workaround
 
Demos:
http://jsfiddle.net/xKh5m/ - uses native setTimeout
http://jsfiddle.net/ujxE3/ - uses workaround timers
*/
 
(function(){
  // Only apply settimeout workaround for iOS 6 - for all others, we map to native Timers
  if (!navigator || !navigator.userAgent.match(/OS 6(_\d)+/i)) return;
  
  // Abort if we're running in a worker. Let's hope workers aren't paused during scrolling!!!
  if (typeof(window) == "undefined")
	  return;
 
  (function (window) {
	  
      // This library re-implements setTimeout, setInterval, clearTimeout, clearInterval for iOS6.
      // iOS6 suffers from a bug that kills timers that are created while a page is scrolling.
      // This library fixes that problem by recreating timers after scrolling finishes (with interval correction).
	// This code is free to use by anyone (MIT, blabla).
	// Author: rkorving@wizcorp.jp

      var timeouts = {};
      var intervals = {};
      var orgSetTimeout = window.setTimeout;
      var orgSetInterval = window.setInterval;
      var orgClearTimeout = window.clearTimeout;
      var orgClearInterval = window.clearInterval;


      function createTimer(set, map, args) {
              var id, cb = args[0], repeat = (set === orgSetInterval);

              function callback() {
                      if (cb) {
                              cb.apply(window, arguments);

                              if (!repeat) {
                                      delete map[id];
                                      cb = null;
                              }
                      }
              }

              args[0] = callback;

              id = set.apply(window, args);

              map[id] = { args: args, created: Date.now(), cb: cb, id: id };

              return id;
      }


      function resetTimer(set, clear, map, virtualId, correctInterval) {
              var timer = map[virtualId];

              if (!timer) {
                      return;
              }

              var repeat = (set === orgSetInterval);

              // cleanup

              clear(timer.id);

              // reduce the interval (arg 1 in the args array)

              if (!repeat) {
                      var interval = timer.args[1];

                      var reduction = Date.now() - timer.created;
                      if (reduction < 0) {
                              reduction = 0;
                      }

                      interval -= reduction;
                      if (interval < 0) {
                              interval = 0;
                      }

                      timer.args[1] = interval;
              }

              // recreate

              function callback() {
                      if (timer.cb) {
                              timer.cb.apply(window, arguments);
                              if (!repeat) {
                                      delete map[virtualId];
                                      timer.cb = null;
                              }
                      }
              }

              timer.args[0] = callback;
              timer.created = Date.now();
              timer.id = set.apply(window, timer.args);
      }


      window.setTimeout = function () {
              return createTimer(orgSetTimeout, timeouts, arguments);
      };


      window.setInterval = function () {
              return createTimer(orgSetInterval, intervals, arguments);
      };

      window.clearTimeout = function (id) {
              var timer = timeouts[id];

              if (timer) {
                      delete timeouts[id];
                      orgClearTimeout(timer.id);
              }
      };

      window.clearInterval = function (id) {
              var timer = intervals[id];

              if (timer) {
                      delete intervals[id];
                      orgClearInterval(timer.id);
              }
      };

      window.addEventListener('scroll', function () {
              // recreate the timers using adjusted intervals
              // we cannot know how long the scroll-freeze lasted, so we cannot take that into account

              var virtualId;

              for (virtualId in timeouts) {
                      resetTimer(orgSetTimeout, orgClearTimeout, timeouts, virtualId);
              }

              for (virtualId in intervals) {
                      resetTimer(orgSetInterval, orgClearInterval, intervals, virtualId);
              }
      });

}(window));
})();