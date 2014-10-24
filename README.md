Twitt Map
=====
Objectives
----
- Use the Elastic Beanstalk API to create, configure, and deploy an application instance programmatically.
- Use the Elastic LoadBalancing API to configure load balancing on Elastic Beanstalk created.
 
Functions
----
 - Reads a stream of tweets from the Twitter Live API (Code provided)
 - Records the tweet ID, time, and other relevant elements into a DB (SQL or NoSQL)
 - Presents the Tweet in a map that is being updated in Near Real Time (Consider evaluating WebSockets, or Server Side Events for your implementation)
 - The map clusters tweets as to show where is people tweeting the most, according to the sample tweets you get from the streaming API.

 Requirements
 ---- 
- Collect about 100MB twits using Twitter API.
- Parse the Twits and store in Dataset. The parsed twits should have location information and a set of key words from the content of the twits.
- You create a scatter plot or any nice plot that depicts all the twits with a the density map - perhaps with color gradient etc. (extra credit - 10 points)
- You should provide a filter that allows a drop down keywords to choose from and only shows twits with those keywords on a google map.

