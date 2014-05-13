
Sarah 'Interactive Email Client' Plugin
=======================================

This plugin lets Sarah check your email accounts interactively.
Just issue a voice command, and Sarah will check your email, and 
report the results (new emails) back to you.


Files
-----

The jar file built by this project needs to be copied to the Sarah plugins directory.
On my computer that directory is _/Users/al/Sarah/plugins/DDInteractiveEmailClient_.

Files in that directory should be:

    InteractiveEmailClient.info
    InteractiveEmailClient.jar
    InteractiveEmailClient.properties

The _InteractiveEmailClient.info_ file currently contains these contents:

    main_class = com.devdaily.sarah.plugin.interactiveemailclient.SarahInteractiveEmailClientPlugin
    plugin_name = Interactive Email Client

The _InteractiveEmailClient.properties_ contains contents like this:

    {
        "whatYouSay" : "check email",
        "accountName": "Ymail Account",
        "username": "YOUR USER ACCOUNT",
        "password": "YOUR PASSWORD",
        "imapServerUrl": "imap.mail.yahoo.com",
        "protocol": "imaps",
        "mailbox": "inbox",
        "usersOfInterest": ["barney", "betty", "fred", "wilma" ]
    }


To-Do
-----

* This module needs a lot of testing


Developers - Building this Plugin
---------------------------------

You can build this plugin using the shell script named _build-jar.sh. It currently looks like this:

    #!/bin/bash

    sbt package

    if [ $? != 0 ]
    then
        echo "'sbt package' failed, exiting now"
        exit 1
    fi

    cp target/scala-2.10/interactiveemailclient_2.10-0.1.jar InteractiveEmailClient.jar

    ls -l InteractiveEmailClient.jar

    echo ""
    echo "Created InteractiveEmailClient.jar. Copy that file to /Users/al/Sarah/plugins/DDInteractiveEmailClient, like this:"
    echo "cp InteractiveEmailClient.jar /Users/al/Sarah/plugins/DDInteractiveEmailClient"


Dependencies
------------

This plugin depends on:

* The Sarah2.jar file.
* The Akka/Scala actors. The actor version needs to be kept in sync with whatever actor version
  Sarah2 uses.

As mentioned above, I need to improve the process of requiring and using the Sarah2.jar file,
but that's more of a problem for the Sarah2 project than for this project. 









