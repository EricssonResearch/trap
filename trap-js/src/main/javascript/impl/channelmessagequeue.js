Trap.ChannelMessageQueue = function()
{
	this.priorities = [];
	this.cPrio = 0;
	this.cPrioIndex = 0;
	this.cPrioBytes = 0;
	
	var RoundRobinChannelSelector = function()
	{
		this.channels = [];
		this.currChannel = 0;
		
		this.getPriority = function()
		{
			return this.channels.length > 0 ? this.channels[0].getPriority() : Number.MIN_VALUE;
		};
		
		this.peek = function()
		{
			try
			{
				var start = this.currChannel;
				var end = this.currChannel + this.channels.length;
				
				for ( var i = start; i < end; i++)
				{
					var m = this.channels[this.currChannel % this.channels.length].peek();
					
					if (m != null) return m;
					
					this.currChannel++;
				}
				
				return null;
			}
			finally
			{
				this.currChannel = this.currChannel % this.channels.length;
			}
		};
		
		this.pop = function()
		{
			var rv = this.channels[this.currChannel].pop();
			this.currChannel++;
			return rv;
		};
		
		this.addChannel = function(c)
		{
			this.channels.push(c);
		};
		
		this.toString = function()
		{
			var sb = new StringBuilder();
			
			sb.append("{");
			
			for ( var i = 0; i < this.channels.length; i++)
			{
				if (i > 0) sb.append(", ");
				sb.append(this.channels[i].toString());
			}
			
			sb.append("}");
			
			return sb.toString();
		};
		
	};
	
	this.rebuild = function(channels)
	{
		var sortedChannels = new Trap.List();
		sortedChannels.addAll(channels);
		sortedChannels.sort(function(a, b) { return b.getPriority() - a.getPriority(); });
		
		var prioList = [];
		var it = sortedChannels.iterator();
		var lastPriority = Number.MIN_VALUE;
		var sel = null;
		
		while (it.hasNext())
		{
			var c = it.next();
			
			if (c.getPriority() != lastPriority)
			{
				sel = new RoundRobinChannelSelector();
				prioList.push(sel);
				lastPriority = c.getPriority();
			}
			sel.addChannel(c);
		}
		
		this.priorities = prioList;
		this.setPrioIndex(0);
	};
	
	this.peek = function()
	{
		
		for ( var i = this.cPrioIndex; i < this.priorities.length; i++)
		{
			var msg = this.priorities[i].peek();
			if (msg != null)
			{
				this.cPrioIndex = i;
				return msg;
			}
		}
		
		return null;
	};
	
	this.pop = function()
	{
		var popped = this.priorities[this.cPrioIndex].pop();
		var bs = popped.length();
		this.cPrioBytes += bs;
		
		if (this.cPrioBytes > this.cPrio) this.setPrioIndex(this.cPrioIndex++);
		
		return popped;
	};
	
	this.rewind = function()
	{
		this.setPrioIndex(0);
	};
	
	this.setPrioIndex = function(idx)
	{
		if (idx < this.priorities.length)
		{
			this.cPrioIndex = idx;
			this.cPrio = this.priorities[this.cPrioIndex].getPriority();
			this.cPrioBytes = 0;
		}
	};
	
	this.toString = function()
	{
		var sb = new Trap.StringBuilder();
		
		sb.append("[\n");
		
		for ( var i = 0; i < this.priorities.length; i++)
		{
			var cs = this.priorities[i];
			
			sb.append("\t");
			sb.append(cs.getPriority());
			sb.append(": ");
			sb.append(cs.toString());
			sb.append("\n");
		}
		
		sb.append("\n]");
		
		return sb.toString();
	};
	
};