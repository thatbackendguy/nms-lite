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

func Start(receiver *zmq4.Socket, sender *zmq4.Socket) error {

	go receive(receiver)

	go send(sender)

	go handleSignals(receiver, sender)

	return nil
}

func handleSignals(receiver *zmq4.Socket, sender *zmq4.Socket) {

	stopReceiver := make(chan os.Signal, 1)

	signal.Notify(stopReceiver, os.Interrupt, syscall.SIGINT, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGKILL)

	receivedSignal := <-stopReceiver

	serverLogger.Trace("Received signal: " + receivedSignal.String())

	Stop(receiver, sender)
}

func Stop(receiver *zmq4.Socket, sender *zmq4.Socket) {

	serverLogger.Info("Stopping ZMQ sockets...")

	if receiver != nil {

		err := receiver.Close()

		if err != nil {

			serverLogger.Error("Failed to close PULL socket: " + err.Error())

		}

	}

	if sender != nil {

		err := sender.Close()

		if err != nil {

			serverLogger.Error("Failed to close PUSH socket: " + err.Error())

		}
	}

	serverLogger.Info("ZMQ sockets closed successfully")

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

			global.Requests <- encodedContext

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

		response := <-global.Responses

		_, err := sender.Send(response, 0)

		if err != nil {

			serverLogger.Error("Error sending response: " + err.Error())

		}
	}
}
