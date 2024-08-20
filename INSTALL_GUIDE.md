# Install guide

## Table of Contents
1. [Obtaining the source code](#obtaining-the-source-code)
2. [Building project](#building-project)
3. [Installing Glassfish](#installing-glassfish)
4. [Preparing the database](#preparing-the-database)
5. [Deploying the project](#deploying-the-project)

## Obtaining the source code

1. Install `git`
2. Run your command line, change directory to the location where you will be building the project (I will be using the directory`D:\forks\build` and run the following commands:
```
git clone https://github.com/hmislk/hmis.git
```

## Building project

**You need to have installed JDK 8+ and Maven on your server to complete this step.**

Now, to build the project, run:
```
cd hmis
mvn package
```

After the successful build you will see the absolute path of the built war file, for example:

`[INFO] Building war: D:\forks\build\hmis\target\arogya2022-3.0.0.war`

## Installing Glassfish

**You need to have JDK 8 on your server for this step** 

I haven't had any success to run the project on Glassfish of version 6+, so the version 5.1.0 is recommended for now.

You can download it [here](https://www.eclipse.org/downloads/download.php?file=/glassfish/glassfish-5.1.0.zip).

Download .zip file and extract it somewhere, for example into `D:\glassfish`

Edit file `D:\glassfish\glassfish\config\asenv.bat` and add following line:

`set AS_JAVA=<JDK8 location>`

In my case: 

`set AS_JAVA=D:\JAVA\jdk1.8.0_252`

Make sure the port 8080 is not being used on your server.

Run you command line, change directory to `<GLASSFISH_HOME>/bin` and run:
```
asadmin start-domain domain1
```

After launch, your can open the Glassfish admin console in your browser: `localhost:4848`

## Preparing the database

I assume you have a MySQL server instance running somewhere, I will be using the MySQL instance running on the same machine.

You need to create an empty database and user with appropriate privileges to access it (both `hmis/hmis` in my case).

After that you need to download JDBC driver jar file, for example, [here](https://mvnrepository.com/artifact/mysql/mysql-connector-java/8.0.18), and put into your glassfish `lib` folder, in my case the path is like that:

`D:\glassfish\glassfish\lib\mysql-connector-java-8.0.18.jar`

Now, restart the Glassfish:

`D:\glassfish\bin\asadmin stop-domain domain1`

`D:\glassfish\bin\asadmin start-domain domain1`

Open glassfish admin console and add the JDBC Connection Pool with settings like these:

![pic1](https://i.ibb.co/SBfsWvg/1.png)

![pic2](https://i.ibb.co/KX3gyWf/2.png)

Change user/password as needed

You need to make sure that test connection to your database is working (ping button in Glassfish)

![pic 3](https://i.ibb.co/Zf3ZCh3/3.png)

If not, I can recommend to copy the file `mysql-connector-java-8.0.18.jar` into directories 

`<GLASSFISH_HOME>\glassfish\domains\domain1\lib`

`<GLASSFISH_HOME>\glassfish\glassfish\domains\domain1\lib`

Restart the Glassfish and try again.

After creating the connection pool resourse you need to create the JNDI resource `jndi/arogya` like that:

![pic 4](https://i.ibb.co/hYp8CQf/4.png)

Now you're ready to deploy the project, the tables in your database will be created automatically.

## Deploying the project

Simply run the command:

`<GLASSFISH_HOME>\bin\asadmin deploy <built war file path>`

In my case:

`D:\glassfish\bin deploy D:\forks\build\hmis\target\arogya2022-3.0.0.war`

When deploying is done, you can open the page `http://localhost:8080/arogya2022/` in your browser, create your first institution/user

![pic 5](https://i.ibb.co/0tgvrcn/5.png)

and you're ready to go!

![pic 6](https://i.ibb.co/1Qv6tsL/6.png)

## Troubleshooting

- I can't access admin pages anymore after creating the first user (which is password/login for the super user?)
