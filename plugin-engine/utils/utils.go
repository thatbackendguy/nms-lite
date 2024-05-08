package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
)

func SendResponse(context map[string]interface{}) {
	jsonOutput, err := json.Marshal(context)

	if err != nil {

		log.Fatal("JSON Marshal Error: ", err)

	}

	encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

	fmt.Println(encodedString)
}
