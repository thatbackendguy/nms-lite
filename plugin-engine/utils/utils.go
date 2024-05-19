package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"plugin-engine/global"
	logger2 "plugin-engine/logger"
)

var logger = logger2.NewLogger(global.LogFilesPath, global.SystemLoggerName)

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
