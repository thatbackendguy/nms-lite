package server

import (
	"collector/logger"
	"github.com/pebbe/zmq4"
	"os"
	"os/signal"
	"syscall"
)

var serverLogger = logger.NewLogger("go-engine/logs", "collector-server")

var (
	Requests = make(chan []byte, 10)
)

func Init() (*zmq4.Socket, error) {

	server, err := zmq4.NewContext()

	if err != nil {

		serverLogger.Error("Failed to create new zmq context")

		return nil, err
	}

	socket, err := server.NewSocket(zmq4.PULL)

	if err != nil {

		serverLogger.Error("Error creating PULL socket: " + err.Error())

		return nil, err
	}

	err = socket.Connect("tcp://localhost:9999")

	if err != nil {

		serverLogger.Error("Connecting pull socket: " + err.Error())

	}

	return socket, nil
}

func Start(puller *zmq4.Socket) (err error) {

	go receive(puller)

	go handleSignals(puller)

	return nil
}

func handleSignals(puller *zmq4.Socket) {

	stopReceiver := make(chan os.Signal, 1)

	signal.Notify(stopReceiver, os.Interrupt, syscall.SIGINT, syscall.SIGTERM, syscall.SIGQUIT, syscall.SIGKILL)

	receivedSignal := <-stopReceiver

	serverLogger.Trace("Received signal: " + receivedSignal.String())

	Stop(puller)
}

func Stop(puller *zmq4.Socket) {

	serverLogger.Info("Stopping ZMQ sockets...")

	if puller != nil {

		err := puller.Close()

		if err != nil {

			serverLogger.Error("Failed to close PULL socket: " + err.Error())

		}
	}

	serverLogger.Info("ZMQ sockets closed successfully")
}

func receive(puller *zmq4.Socket) {

	for {
		encodedContext, err := puller.RecvBytes(0)

		if err != nil {

			serverLogger.Error("Error receiving encodedContext: " + err.Error())

		}

		if len(encodedContext) > 0 {

			Requests <- encodedContext
		}
	}
}
