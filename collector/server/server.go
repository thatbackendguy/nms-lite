package server

import (
	"collector/logger"
	"encoding/json"
	"fmt"
	"github.com/pebbe/zmq4"
)

type Server struct {
	context *zmq4.Context

	receiver *zmq4.Socket
}

var (
	Requests = make(chan map[string]interface{}, 100)
)

var serverLogger = logger.GetLogger("go-engine/logs", "server")

func (server *Server) Init() (err error) {

	server.context, err = zmq4.NewContext()

	if err != nil {

		serverLogger.Error("Failed to create new zmq context: " + err.Error())

		return
	}

	server.receiver, err = server.context.NewSocket(zmq4.PULL)

	if err != nil {

		serverLogger.Error("Error creating PULL socket: " + err.Error())

		return
	}

	err = server.receiver.Connect("tcp://localhost:9999")

	if err != nil {

		serverLogger.Error("Connecting pull socket: " + err.Error())

		return
	}

	go server.Receive()

	return nil
}

func (server *Server) Receive() {

	defer func() {
		if err := recover(); err != nil {
			serverLogger.Error(fmt.Sprintf("Panic occured while receiving messages: %v", err))

			server.Receive()
		}
	}()

	for {
		data, err := server.receiver.RecvBytes(0)

		if err != nil {

			serverLogger.Error("Error receiving encodedContext: " + err.Error())

			continue
		}

		record := make(map[string]interface{})

		err = json.Unmarshal(data, &record)

		if err != nil {

			serverLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			continue

		}

		if len(record) > 0 {

			Requests <- record

		}
	}
}

func (server *Server) Stop() {

	err := server.receiver.Close()
	if err != nil {

		serverLogger.Error("Error closing receiver: " + err.Error())

		return

	}

	err = server.context.Term()
	if err != nil {

		serverLogger.Error("Error closing context: " + err.Error())

		return

	}

	return
}
