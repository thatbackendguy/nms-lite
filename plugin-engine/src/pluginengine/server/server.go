package server

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/pebbe/zmq4"
	"plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/utils"
)

type Server struct {
	context *zmq4.Context

	receiver, sender *zmq4.Socket
}

var serverLogger = utils.GetLogger(consts.LogFilesPath, "server")

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

	err = server.receiver.Connect("tcp://localhost:7777")

	if err != nil {

		serverLogger.Error("Connecting pull socket: " + err.Error())

		return
	}

	server.sender, err = server.context.NewSocket(zmq4.PUSH)

	if err != nil {

		serverLogger.Error("Error creating PUSH socket: " + err.Error())

		return
	}

	err = server.sender.Connect("tcp://localhost:8888")

	if err != nil {

		serverLogger.Error("Connecting push socket: " + err.Error())

		return
	}

	go server.Receive()

	go server.Send()

	return nil
}

func (server *Server) Receive() {

	// receiving go routine will not stop if there is panic while receiving or parsing json
	defer func() {
		if err := recover(); err != nil {
			serverLogger.Error(fmt.Sprintf("Panic occured while receiving messages: %v", err))

			server.Receive()
		}
	}()

	for {
		encodedContext, err := server.receiver.Recv(0)

		if err != nil {

			serverLogger.Error("Error receiving encodedContext: " + err.Error())

			continue
		}

		contexts := make([]map[string]interface{}, 0)

		decodedContext, err := base64.StdEncoding.DecodeString(encodedContext)

		if err != nil {

			serverLogger.Error(fmt.Sprintf("base64 decoding error: %s", err.Error()))

			continue

		}

		err = json.Unmarshal(decodedContext, &contexts)

		if err != nil {

			serverLogger.Error(fmt.Sprintf("unable to convert JSON string to map: %s", err.Error()))

			continue

		}

		if len(contexts) > 0 {

			consts.Requests <- contexts

		}
	}
}

func (server *Server) Send() {

	// sending go routine will not stop if there is panic marshaling json or sending data
	defer func() {
		if err := recover(); err != nil {
			serverLogger.Error(fmt.Sprintf("Panic occured while sending messages: %v", err))

			server.Send()
		}
	}()

	for {

		response := <-consts.Responses

		jsonOutput, err := json.Marshal(response)

		if err != nil {

			serverLogger.Error("JSON Marshal Error: " + err.Error())

			continue

		}

		encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

		serverLogger.Debug(encodedString)

		_, err = server.sender.Send(encodedString, 0)

		if err != nil {

			serverLogger.Error("Error sending response: " + err.Error())

		}
	}
}

func (server *Server) Stop() {

	err := server.receiver.Close()

	if err != nil {

		serverLogger.Error("Error closing receiver: " + err.Error())

		return

	}

	err = server.sender.Close()

	if err != nil {

		serverLogger.Error("Error closing sender: " + err.Error())

		return

	}

	err = server.context.Term()

	if err != nil {

		serverLogger.Error("Error closing context: " + err.Error())

		return

	}

	close(consts.Responses)

	close(consts.Requests)

	return
}
