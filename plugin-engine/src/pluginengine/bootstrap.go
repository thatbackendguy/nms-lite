package main

import (
	"fmt"
	. "plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/engine"
	"plugin-engine/src/pluginengine/server"
	"plugin-engine/src/pluginengine/utils"
)

var PluginEngineLogger = utils.GetLogger(LogFilesPath, SystemLoggerName)

func main() {

	PluginEngineLogger.Info("Starting Plugin Engine")

	// fatal error, program execution will stop
	defer func() {

		if err := recover(); err != nil {

			PluginEngineLogger.Error(fmt.Sprintf("Panic recovered: %v", err))
		}

		close(Requests)

		close(Responses)
	}()

	zmqClient := server.Server{}

	err := zmqClient.Init()

	defer zmqClient.Stop()

	if err != nil {

		PluginEngineLogger.Error("ZMQ server failed to start: " + err.Error())

		return

	}

	engine.Start()
}
