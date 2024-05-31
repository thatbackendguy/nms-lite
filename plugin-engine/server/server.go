package server

import (
	zmq "github.com/pebbe/zmq4"
	"plugin-engine/global"
	"plugin-engine/logger"
)

var serverLogger = logger.NewLogger(global.LogFilesPath, "server")

func Init() (puller *zmq.Socket, pusher *zmq.Socket, err error) {
	server, err := zmq.NewContext()

	if err != nil {
		serverLogger.Error("Failed to create new zmq context")

		return nil, nil, err
	}

	defer func(server *zmq.Context) {
		if server != nil {

			err := server.Term()

			if err != nil {

				serverLogger.Error(err.Error())

			}
		}
	}(server)

	puller, err = server.NewSocket(zmq.PULL)

	if err != nil {

		serverLogger.Error(err.Error())

		return nil, nil, err
	}

	defer func(puller *zmq.Socket) {

		err := puller.Close()

		if err != nil {

			serverLogger.Error(err.Error())

		}
	}(puller)

	pusher, err = server.NewSocket(zmq.PUSH)

	if err != nil {

		serverLogger.Error(err.Error())

		return nil, nil, err
	}

	defer func(pusher *zmq.Socket) {

		err := pusher.Close()

		if err != nil {

			serverLogger.Error(err.Error())

		}
	}(pusher)

	err = puller.Connect("tcp://localhost:7777")

	if err != nil {

		serverLogger.Error(err.Error())

	}

	err = pusher.Connect("tcp://localhost:8888")

	if err != nil {

		serverLogger.Error(err.Error())

	}

	return pusher, puller, nil
}

func Start(puller *zmq.Socket, pusher *zmq.Socket) (err error) {
	go receive(puller)

	go send(pusher)

	return nil
}

func receive(puller *zmq.Socket) {
	for {
		encodedContext, err := puller.Recv(0)

		if err != nil {

			serverLogger.Error(err.Error())

		}

		global.Requests <- encodedContext
	}
}

func send(pusher *zmq.Socket) {
	for {
		response := <-global.Responses

		_, err := pusher.Send(response, 0)

		if err != nil {

			serverLogger.Error(err.Error())

		}
	}
}
