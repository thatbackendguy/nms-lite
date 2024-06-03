package utils

import (
	"os/exec"
	"strings"
)

func CheckAvailability(context map[string]interface{}) {

	command := "fping"

	args := []string{context["object.ip"].(string), "-c3", "-q"}

	execute := exec.Command(command, args...)

	output, _ := execute.CombinedOutput()

	status := "down"

	if strings.Contains(string(output), "/0%") {

		status = "up"

	}

	context["result"] = map[string]interface{}{

		"is.available": status,
	}
}
