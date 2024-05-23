import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

import static com.motadata.constants.Constants.*;

public class StoreRemoteMetrics
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreRemoteMetrics.class);

    private static final String DB_URI = "jdbc:mysql://localhost:3306/configDB";

    private static final String DB_USERNAME = "root";

    private static final String DB_PASS = "Root@1010";

    private static final String METRICS_INSERT_QUERY = "INSERT INTO network_interface (`object.ip`,`interface.index`,`interface.name`,`interface.operational.status`,`interface.admin.status`,`interface.description`,`interface.sent.error.packet`,`interface.received.error.packet`,`interface.sent.octets`,`interface.received.octets`,`interface.speed`,`interface.alias`,`interface.physical.address`,`poll.time`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    public static void main(String[] args)
    {

        try (ZContext context = new ZContext(); )
        {
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);

            socket.bind("tcp://*:9090");

            while (true)
            {
                var decodedString = new String(Base64.getDecoder().decode(socket.recvStr(0)));

                var result = new JsonArray(decodedString);

                for(var data : result)
                {
                    var monitor = new JsonObject(data.toString());

                    var interfaceData = monitor.getJsonObject(RESULT).getJsonArray(INTERFACE);

                    var objectIp = monitor.getString(OBJECT_IP);

                    var pollTime = monitor.getString(POLL_TIME);

                    try(var conn = DriverManager.getConnection(DB_URI,DB_USERNAME,DB_PASS); var insertPolledMetrics = conn.prepareStatement(METRICS_INSERT_QUERY))
                    {
                        for(var singleInterface : interfaceData)
                        {
                            var interfaceObj = new JsonObject(singleInterface.toString());

                            insertPolledMetrics.setString(1, objectIp);

                            insertPolledMetrics.setInt(2, interfaceObj.containsKey(INTERFACE_INDEX) ? interfaceObj.getInteger(INTERFACE_INDEX) : -999);
                            insertPolledMetrics.setString(3, interfaceObj.containsKey(INTERFACE_NAME) ? interfaceObj.getString(INTERFACE_NAME) : "");
                            insertPolledMetrics.setInt(4, interfaceObj.containsKey(INTERFACE_OPERATIONAL_STATUS) ? interfaceObj.getInteger(INTERFACE_OPERATIONAL_STATUS) : -999);
                            insertPolledMetrics.setInt(5, interfaceObj.containsKey(INTERFACE_ADMIN_STATUS) ? interfaceObj.getInteger(INTERFACE_ADMIN_STATUS) : -999);
                            insertPolledMetrics.setString(6, interfaceObj.containsKey(INTERFACE_DESCRIPTION) ? interfaceObj.getString(INTERFACE_DESCRIPTION) : "");
                            insertPolledMetrics.setInt(7, interfaceObj.containsKey(INTERFACE_SENT_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_SENT_ERROR_PACKET) : -999);
                            insertPolledMetrics.setInt(8, interfaceObj.containsKey(INTERFACE_RECEIVED_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_RECEIVED_ERROR_PACKET) : -999);
                            insertPolledMetrics.setInt(9, interfaceObj.containsKey(INTERFACE_SENT_OCTETS) ? interfaceObj.getInteger(INTERFACE_SENT_OCTETS) : -999);
                            insertPolledMetrics.setInt(10, interfaceObj.containsKey(INTERFACE_RECEIVED_OCTETS) ? interfaceObj.getInteger(INTERFACE_RECEIVED_OCTETS) : -999);
                            insertPolledMetrics.setInt(11, interfaceObj.containsKey(INTERFACE_SPEED) ? interfaceObj.getInteger(INTERFACE_SPEED) : -999);
                            insertPolledMetrics.setString(12, interfaceObj.containsKey(INTERFACE_ALIAS) ? interfaceObj.getString(INTERFACE_ALIAS) : "");
                            insertPolledMetrics.setString(13, interfaceObj.containsKey(INTERFACE_PHYSICAL_ADDRESS) ? interfaceObj.getString(INTERFACE_PHYSICAL_ADDRESS) : "");
                            insertPolledMetrics.setString(14, pollTime);

                            insertPolledMetrics.addBatch();
                        }

                        var rowsInserted = insertPolledMetrics.executeBatch();

                        LOGGER.trace("{} rows data inserted for {}", rowsInserted.length, objectIp);

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());
                    }
                }

            }
        }
        catch (Exception exception)
        {
            LOGGER.error(ERROR_CONTAINER, exception.getMessage());
        }

    }

}