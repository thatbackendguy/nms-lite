package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
)

var pluginEngineLogger = NewLogger(LogFilesPath, "bootstrap")

func SendResponse(context []map[string]interface{}) {

	jsonOutput, err := json.Marshal(context)

	if err != nil {

		log.Fatal("JSON Marshal Error: ", err)

	}

	encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

	pluginEngineLogger.Debug(encodedString)

	fmt.Println(encodedString)
}
