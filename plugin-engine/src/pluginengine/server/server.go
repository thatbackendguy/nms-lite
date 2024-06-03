package server

import (
	"encoding/base64"
	"encoding/json"
	"github.com/pebbe/zmq4"
	"plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/utils"
)

var serverLogger = utils.GetLogger(consts.LogFilesPath, "server")

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

func Start() error {

	receiver, sender, err := Init()

	if err != nil {

		serverLogger.Error("Error creating zmq sockets server" + err.Error())

		return err

	}

	go receive(receiver)

	go send(sender)

	return nil
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

			consts.Requests <- encodedContext

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

		response := []map[string]interface{}{
			<-consts.Responses,
		}

		jsonOutput, err := json.Marshal(response)

		if err != nil {

			serverLogger.Error("JSON Marshal Error: " + err.Error())

			continue

		}

		encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

		serverLogger.Debug(encodedString)

		_, err = sender.Send(encodedString, 0)

		if err != nil {

			serverLogger.Error("Error sending response: " + err.Error())

		}
	}
}
