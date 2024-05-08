package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"plugin-engine/constants"
	"plugin-engine/requesttype"
	"time"
)

func main() {

	file, err := os.OpenFile("pluginEngine.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)

	if err != nil {
		log.Fatal(err)
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

		log.Fatal("Unable to convert string to json map: ", err)

		return

	}

	switch context[constants.REQUEST_TYPE].(string) {

	case constants.DISCOVERY:

		requesttype.Discovery(context)

	case constants.COLLECT:

		requesttype.Collect(context)
	}

	log.Println(time.Now(), context)

	jsonOutput, err := json.Marshal(context)

	if err != nil {

		log.Fatal("json marshal error: ", err)

	}

	encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

	fmt.Println(encodedString)
}
