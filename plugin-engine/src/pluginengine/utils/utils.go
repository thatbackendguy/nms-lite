package utils

import (
	"fmt"
	"os/exec"
	"plugin-engine/src/pluginengine/consts"
	"strings"
)

var utilsLogger = GetLogger(consts.LogFilesPath, "utils")

func CheckAvailability(context map[string]interface{}) {

	defer func() {
		if err := recover(); err != nil {

			utilsLogger.Error(fmt.Sprintf("Panic recovered in availability: %v", err))

			errors := map[string]string{

				consts.Error: "Internal Server Error",

				consts.ErrorCode: "500",

				consts.ErrorMsg: fmt.Sprintf("Error while checking availability: %v", err),
			}

			context[consts.Error] = errors

			context[consts.Status] = consts.Failed

			consts.Responses <- context
		}

		return
	}()

	command := "fping"

	args := []string{context[consts.ObjectIp].(string), "-c3", "-q"}

	execute := exec.Command(command, args...)

	output, _ := execute.CombinedOutput()

	status := "down"

	if strings.Contains(string(output), "/0%") {

		status = "up"

	}

	context[consts.Result] = map[string]interface{}{

		"is.available": status,
	}

	consts.Responses <- context

	return
}
