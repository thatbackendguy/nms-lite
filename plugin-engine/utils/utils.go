package utils

import (
	"os/exec"
	"plugin-engine/global"
	"plugin-engine/logger"
	"plugin-engine/plugins/snmp"
	"strings"
)

var utilsLogger = logger.NewLogger(global.LogFilesPath, "utils")

func CheckAvailability(context map[string]interface{}) {

	command := "fping"

	args := []string{context[snmp.ObjectIp].(string), "-c3", "-q"}

	execute := exec.Command(command, args...)

	output, _ := execute.CombinedOutput()

	status := "down"

	if strings.Contains(string(output), "/0%") {

		status = "up"

	}

	context[global.Result] = map[string]interface{}{
		"is.available": status,
	}
}
