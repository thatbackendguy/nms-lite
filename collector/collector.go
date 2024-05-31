package main

import (
	"collector/logger"
	"encoding/json"
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"os"
	"path/filepath"
	"strings"
)

var collectorLogger = logger.NewLogger("go-engine/logs", "collector")

func main() {
	zmqContext, _ := zmq.NewContext()

	defer zmqContext.Term()

	socket, _ := zmqContext.NewSocket(zmq.PULL)

	defer socket.Close()

	socket.Connect("tcp://localhost:9999")

	works := make(chan []byte, 10)

	go func() {
		for {
			data, err := socket.RecvBytes(0)

			if err != nil {
				collectorLogger.Error(err.Error())
			}

			works <- data
		}
	}()

	for data := range works {

		contexts := make([]map[string]interface{}, 0)

		err := json.Unmarshal(data, &contexts)

		if err != nil {

			collectorLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			break
		}

		for _, result := range contexts {
			if strings.EqualFold(result["status"].(string), "success") {

				pollTime := result["poll.time"].(string)

				objectIp := result["object.ip"].(string)

				if interfacesData, ok := result["result"].(map[string]interface{})["interface"].([]interface{}); ok {

					for _, value := range interfacesData {

						if interfaceData, ok := value.(map[string]interface{}); ok {

							interfaceName := interfaceData["interface.name"].(string)

							folderPath := "./metrics-result/" + objectIp + "/"

							err = os.MkdirAll(fmt.Sprintf(folderPath), 0755)

							if err != nil {

								collectorLogger.Trace("Error creating folder:" + err.Error())

								continue
							}

							collectorLogger.Trace("Folder created successfully")

							record := map[string]interface{}{

								pollTime: interfaceData,
							}

							filePath := filepath.Join(folderPath, strings.ReplaceAll(strings.ReplaceAll(interfaceName, "/", "-"), ".", "-"))

							file, err := os.OpenFile(filePath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)

							if err != nil {

								collectorLogger.Error("Error opening file: " + filePath)

								continue
							}

							recordStr, err := json.Marshal(record)

							if err != nil {

								collectorLogger.Error("Error marshaling data:" + err.Error())

								continue
							}

							_, err = file.Write(append(recordStr, 10))

							if err != nil {

								collectorLogger.Error("Error writing to file:" + err.Error())

								continue
							}

							collectorLogger.Trace("Data appended to file successfully")

							err = file.Close()

							if err != nil {

								collectorLogger.Error("Error closing file:" + err.Error())

								continue
							}
						}
					}
				}

			}

		}
	}
}
