# Use the official ZooKeeper image as a base
FROM zookeeper:3.9.2

# Set standard environment variables
ENV ZOO_MY_ID=1
ENV ZOO_PORT=2181

# Expose the default ZooKeeper port
EXPOSE 2181

# The base image already defines:
# VOLUME ["/data", "/datalog"]
# ENTRYPOINT ["/docker-entrypoint.sh"]
# CMD ["zkServer.sh", "start-foreground"]

# By default, building and running this image will start a single-node ZooKeeper server.
