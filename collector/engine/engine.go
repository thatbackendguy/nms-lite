package engine

import (
	"collector/logger"
	"collector/server"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

var engineLogger = logger.GetLogger("go-engine/logs", "collector-engine")

func Start() {
	defer func() {
		if err := recover(); err != nil {

			engineLogger.Error(fmt.Sprintf("recovered from panic: %v", err))

			Start()
		}
	}()

	for {
		data := <-server.Requests

		if strings.EqualFold(data["status"].(string), "success") {

			pollTime := data["poll.time"].(string)

			objectIp := data["object.ip"].(string)

			if interfacesData, ok := data["result"].(map[string]interface{})["interface"].([]interface{}); ok {

				for _, value := range interfacesData {

					if interfaceData, ok := value.(map[string]interface{}); ok {

						interfaceName, nameExists := interfaceData["interface.name"].(string)

						if !nameExists || interfaceName == "" {

							interfaceName, _ = interfaceData["interface.description"].(string)

						}

						folderPath := "./metrics-result/" + objectIp + "/"

						err := os.MkdirAll(fmt.Sprintf(folderPath), 0755)

						if err != nil {

							engineLogger.Trace("Error creating folder:" + err.Error())

							continue
						}

						engineLogger.Trace("Folder created successfully")

						record := map[string]interface{}{

							pollTime: interfaceData,
						}

						filePath := filepath.Join(folderPath, strings.ReplaceAll(strings.ReplaceAll(interfaceName, "/", "-"), ".", "-"))

						file, err := os.OpenFile(filePath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)

						if err != nil {

							engineLogger.Error("Error opening file: " + filePath)

							continue
						}

						recordStr, err := json.Marshal(record)

						if err != nil {

							engineLogger.Error("Error marshaling data:" + err.Error())

							continue
						}

						_, err = file.Write(append(recordStr, 10))

						if err != nil {

							engineLogger.Error("Error writing to file:" + err.Error())

							continue
						}

						engineLogger.Trace("Data appended to file successfully")

						err = file.Close()

						if err != nil {

							engineLogger.Error("Error closing file:" + err.Error())

							continue
						}
					}
				}
			}
		}
	}
}
