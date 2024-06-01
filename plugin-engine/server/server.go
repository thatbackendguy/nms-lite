package server

import (
	"github.com/pebbe/zmq4"
	"os"
	"os/signal"
	"plugin-engine/global"
	"plugin-engine/logger"
	"syscall"
)

var serverLogger = logger.NewLogger(global.LogFilesPath, "server")

func Init() (*zmq4.Socket, *zmq4.Socket, error) {

	server, err := zmq4.NewContext()

	if err != nil {

		serverLogger.Error("Failed to create new zmq context")

		return nil, nil, err
	}

	puller, err := server.NewSocket(zmq4.PULL)

	if err != nil {

		serverLogger.Error("Error creating PULL socket: " + err.Error())

		return nil, nil, err
	}

	err = puller.Connect("tcp://localhost:9001")

	if err != nil {

		serverLogger.Error("Connecting pull socket: " + err.Error())

	}

	pusher, err := server.NewSocket(zmq4.PUSH)

	if err != nil {

		serverLogger.Error("Error creating PUSH socket: " + err.Error())

		return nil, nil, err
	}

	err = pusher.Connect("tcp://localhost:9002")

	if err != nil {

		serverLogger.Error("Connecting push socket: " + err.Error())

	}

	return pusher, puller, nil
}

func Start(puller *zmq4.Socket, pusher *zmq4.Socket) (err error) {

	go receive(puller)

	go send(pusher)

	go handleSignals(puller, pusher)

	return nil
}

func handleSignals(puller *zmq4.Socket, pusher *zmq4.Socket) {

	stopReceiver := make(chan os.Signal, 1)

	signal.Notify(stopReceiver, os.Interrupt, syscall.SIGINT, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGKILL)

	receivedSignal := <-stopReceiver

	serverLogger.Trace("Received signal: " + receivedSignal.String())

	Stop(puller, pusher)
}

func Stop(puller *zmq4.Socket, pusher *zmq4.Socket) {

	serverLogger.Info("Stopping ZMQ sockets...")

	if puller != nil {

		err := puller.Close()

		if err != nil {

			serverLogger.Error("Failed to close PULL socket: " + err.Error())

		}
	}

	if pusher != nil {

		err := pusher.Close()

		if err != nil {

			serverLogger.Error("Failed to close PUSH socket:" + err.Error())

		}
	}

	serverLogger.Info("ZMQ sockets closed successfully")
}

func receive(puller *zmq4.Socket) {
	for {
		encodedContext, err := puller.Recv(0)

		if err != nil {

			serverLogger.Error("Error receiving encodedContext: " + err.Error())

		}

		if encodedContext != "" {

			global.Requests <- encodedContext
		}
	}
}

func send(pusher *zmq4.Socket) {
	for {
		response := <-global.Responses

		_, err := pusher.Send(response, 0)

		if err != nil {

			serverLogger.Error(err.Error())

		}
	}
}
