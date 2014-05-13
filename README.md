
Sarah 'Interactive Email Client' Plugin
=======================================

This plugin lets Sarah check your email accounts interactively.
Just issue a voice command, and Sarah will check your email, and 
report the results (new emails) back to you.

I need to add more docs here. I'll get to it when I can. Today I'm
just trying to get everything into Github. As a few quick notes:

* See the config file in the `data` directory.
* In that file, `usersOfInterest` is an array of users that, when they
  send you an email, you want Sarah to let you know about it. This
  gives you a way of getting notifications about some users, but not
  all users.


Developers
----------

* To create a jar file, see the script in the deploy/ directory.
* You should also be able to create a jar file more easily now
  by running `sbt package`, but i haven't tested that yet.


