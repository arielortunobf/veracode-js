# integration-ftps

# build file
mvn clean install

# can be run like the other mobi applications
environment to use (change the server port as needed)
ENV_NAME=local;MC_SECRET=ZPNt3TXq6HnZnVBP;MONGO_URL=mongodb://127.0.0.1:27017/emi;MQ_AXWAYURL=tcp://127.0.0.1:61616;MQ_URL=tcp://127.0.0.1:61616;SERVER_PORT=8099;SPRING_PROFILES_ACTIVE=docker;SUB_SYSTEM=local