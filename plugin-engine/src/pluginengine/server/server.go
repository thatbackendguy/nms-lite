package server

import (
	"github.com/pebbe/zmq4"
	. "plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/utils"
)

var serverLogger = utils.GetLogger(LogFilesPath, "server")

func Init() (receiver *zmq4.Socket, sender *zmq4.Socket, err error) {

	server, err := zmq4.NewContext()

	if err != nil {

		serverLogger.Error("Failed to create new zmq context: " + err.Error())

		return nil, nil, err
	}

	receiver, err = server.NewSocket(zmq4.PULL)

	if err != nil {
		serverLogger.Error("Error creating PULL socket: " + err.Error())

		return nil, nil, err
	}

	sender, err = server.NewSocket(zmq4.PUSH)

	if err != nil {

		serverLogger.Error("Error creating PUSH socket: " + err.Error())

		return nil, nil, err
	}

	return receiver, sender, nil
}

func Start(receiver *zmq4.Socket, sender *zmq4.Socket) {

	go receive(receiver)

	go send(sender)
}

func receive(receiver *zmq4.Socket) {

	err := receiver.Connect("tcp://localhost:7777")

	if err != nil {

		serverLogger.Error("Connecting pull socket: " + err.Error())

		return
	}

	for {
		encodedContext, err := receiver.Recv(0)

		if err != nil {

			serverLogger.Error("Error receiving encodedContext: " + err.Error())

			continue
		}

		if encodedContext != "" {

			Requests <- encodedContext

		}
	}
}

func send(sender *zmq4.Socket) {

	err := sender.Connect("tcp://localhost:8888")

	if err != nil {

		serverLogger.Error("Connecting push socket: " + err.Error())

		return
	}

	for {

		response := <-Responses

		_, err := sender.Send(response, 0)

		if err != nil {

			serverLogger.Error("Error sending response: " + err.Error())

		}
	}
}
