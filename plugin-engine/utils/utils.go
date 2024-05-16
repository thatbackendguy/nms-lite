package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
)

var logger = NewLogger(LogFilesPath, SystemLoggerName)

func SendResponse(context []map[string]interface{}) {

	jsonOutput, err := json.Marshal(context)

	if err != nil {

		log.Fatal("JSON Marshal Error: ", err)

	}

	logger.Debug(string(jsonOutput))

	encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

	logger.Info(encodedString)

	fmt.Println(encodedString)
}
