package main

import (
	"collector/engine"
	"collector/logger"
	"collector/server"
)

var collectorLogger = logger.GetLogger("go-engine/logs", "collector")

func main() {

	zmqClient := server.Server{}

	err := zmqClient.Init()

	if err != nil {

		collectorLogger.Error("ZMQ Server not initialized: " + err.Error())
	}

	defer zmqClient.Stop()

	engine.Start()
}
