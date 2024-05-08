package main

import (
	"encoding/base64"
	"encoding/json"
	"log"
	"os"
	"plugin-engine/constants"
	"plugin-engine/requesttype"
	"plugin-engine/utils"
	"time"
)

func main() {

	file, err := os.OpenFile("pluginEngine.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)

	if err != nil {

		log.Fatal(err)

		return
	}

	defer file.Close()

	// Set the output of the logger to the file

	log.SetOutput(file)

	decodedBytes, err := base64.StdEncoding.DecodeString(os.Args[1])

	if err != nil {

		log.Fatal("Base64 decoding error: ", err)

		return

	}

	var context map[string]interface{}

	err = json.Unmarshal(decodedBytes, &context)

	if err != nil {

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err

		errMap[constants.ERROR_CODE] = "JSON01"

		errMap[constants.ERROR_MSG] = "Unable to convert JSON string to map"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Unable to convert JSON string to map: ", err)

		return

	}

	switch context[constants.REQUEST_TYPE].(string) {

	case constants.DISCOVERY:

		requesttype.Discovery(context)

	case constants.COLLECT:

		requesttype.Collect(context)
	}

	log.Println(time.Now(), context)

	utils.SendResponse(context)
}
