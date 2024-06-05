package main

import (
	"collector/logger"
	"collector/server"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

var collectorLogger = logger.NewLogger("go-engine/logs", "collector")

func main() {

	puller, err := server.Init()

	if err != nil {

		collectorLogger.Error("ZMQ Server not initialized: " + err.Error())
	}

	err = server.Start(puller)

	if err != nil {

		collectorLogger.Error("ZMQ not connected: " + err.Error())

	}

	for {
		data := <-server.Requests

		result := make(map[string]interface{})

		err := json.Unmarshal(data, &result)

		if err != nil {

			collectorLogger.Error("unable to convert JSON string to map:" + err.Error())

			break
		}

		if strings.EqualFold(result["status"].(string), "success") {

			pollTime := result["poll.time"].(string)

			objectIp := result["object.ip"].(string)

			if interfacesData, ok := result["result"].(map[string]interface{})["interface"].([]interface{}); ok {

				for _, value := range interfacesData {

					if interfaceData, ok := value.(map[string]interface{}); ok {

						interfaceName, nameExists := interfaceData["interface.name"].(string)

						if !nameExists || interfaceName == "" {

							interfaceName, _ = interfaceData["interface.description"].(string)

						}

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
