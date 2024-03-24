# Sample deployment using containers
This folder is a sample on how to quickly spin-up (or deploy) the HMIS app together with a database. By following this guide you do not have to run java, a database or a webserver locally on your computer. Everything comes out of the box and can be cleaned up quickly.

## Requirements
- docker
- internet connection
- open ports: 3306, 4848, 8080

## How to use
All commands in subchapters assume you are in the `deployment` directory.

### Starting the environment
```
docker compose up --build
```

After some time the app will be automatically deployed to the server, including the creation of connection pool and JDBC resource. You can find the app at `localhost:8080/horizon`.

### Stopping the environment
```
docker compose down
```
To remove the entire database (to start fresh) add ` --volumes; rm -r .data` to the end of the stop command.

### Other tips
- It is still possible to interact with the admin console of the payara (glassfish) webserver at `localhost:4848`. The username for the root user is `admin` and the password is `admin`.
- You can open a mysql client connection by 'stepping into' the mysql container by running `docker exec -it deployment_db_1 bash` and once you are inside the mysql container run `mysql -ppassword` ('password' is the password here). You can also use any other sql tool and connect to localhost:3306.