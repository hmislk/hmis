# Install guide

## Table of Contents
1. [Obtaining the source code](#obtaining-the-source-code)
2. [Installing Maven](#installing-maven)
3. [Building project](#building-project)
4. [Installing Glassfish](#installing-glassfish)
5. [Preparing the database](#preparing-the-database)
6. [Deploying the project](#deploying-the-project)

## Obtaining the source code

1. Install `git`
2. Run your command line, change directory to the location where you will be building the project (I will be using the directory`D:\forks\build` and run the following commands:
```
git clone https://github.com/hmislk/hmis.git
```

## Installing Maven

On Debian-based systems, install Maven via:

```
sudo apt-get install maven
```

For other operating systems, download Maven from the [official website](https://maven.apache.org/download.cgi) and follow the provided installation instructions.

## Building project

**You need to have installed JDK 11+ and Maven on your server to complete this step.**

Now, to build the project, run:
```
cd hmis
mvn package
```

After the successful build you will see the absolute path of the built war file, for example:

`[INFO] Building war: D:\forks\build\hmis\target\arogya2022-3.0.0.war`

## Installing Payara Server

**You need to have JDK 11 on your server for this step** 

Payara server verison 5.2022.5 is the highest tested version and therefore it is recommended.

You can download it [here](https://nexus.payara.fish/#browse/browse:payara-community:fish%2Fpayara%2Fdistributions%2Fpayara%2F5.2022.5%2Fpayara-5.2022.5.zip).

Download .zip file and extract it where you want your server files to be, for example into `D:\Payara`

Edit the Payara configuration file to set the JDK path:

**For Windows (`D:\Payara\payara5\glassfish\config\asenv.bat`):**
```
set AS_JAVA=D:\JAVA\jdk-11.0.25
```

**For Linux (`/path/to/payara/glassfish/config/asenv.conf`):**
```
AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"
```

Make sure the port 8080 is not being used on your server.

In your command line, change directory to `<PAYARA_HOME>/bin` and run:
```
asadmin start-domain domain1
```

After launch, your can open the Payara admin console in your browser: `localhost:4848`

## Preparing the database

I assume you have a MySQL server instance running somewhere, I will be using the MySQL instance running on the same machine.

You need to create an empty database (named `hmis`) and a user with appropriate privileges to access it (username/password both as `hmis/hmis` in my case).

After that you need to download JDBC driver jar file, for example, [here](https://mvnrepository.com/artifact/mysql/mysql-connector-java/8.0.30), and put into your `D:\Payara\payara5\glassfish\lib` folder, in my case the path is like that:

`D:\Payara\payara5\glassfish\lib\mysql-connector-java-8.0.30.jar`

Now, restart the Glassfish:

`D:\Payara\payara5\bin\asadmin stop-domain domain1`

`D:\Payara\payara5\bin\asadmin start-domain domain1`

Open Payara admin console and create a new JDBC Connection Pool with settings like these:

![pic1](https://i.ibb.co/bvr5bp2/Screenshot-2024-12-09-at-21-59-55.png)

![pic2](https://i.ibb.co/Nxtr1FV/Screenshot-2024-12-09-at-22-01-26.png)

Change username/password as needed.

You need to make sure that test connection to your database is working (Ping button to the left).

![pic 3](https://i.ibb.co/7vVQD87/Screenshot-2024-12-09-at-22-55-13.png)

If not, I can recommend to copy the file `mysql-connector-java-8.0.30.jar` into directory `<PAYARA_HOME>\glassfish\lib`.

Restart the Payara server and try again.

After creating the connection pool resource, you need to create the JNDI resource `jdbc/Ruhunu` as follows:

![pic 4](https://i.ibb.co/ZJ7LhGm/Screenshot-2024-12-09-at-22-55-37.png)

Now you're ready to deploy the project and the tables in your database will be created automatically.

## Deploying the project

Simply run the command:

`<PAYARA_HOME>\bin\asadmin deploy <built war file path>`

In my case:

`D:\Payara\payara5\bin\asadmin deploy D:\forks\build\hmis\target\arogya2022-3.0.0.war`

When deploying is done, you can open the page `http://localhost:8080/arogya2022/` in your browser, create your first institution/user

![pic 5](https://i.ibb.co/0tgvrcn/5.png)

and you're ready to go!

![pic 6](https://i.ibb.co/1Qv6tsL/6.png)

## Troubleshooting

- I can't access admin pages anymore after creating the first user (which is password/login for the super user?)
 
