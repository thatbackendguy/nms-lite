package utils

import (
	"fmt"
	"os"
	"time"
)

const (
	Info  = 2
	Debug = 1
	Trace = 0
)

type Logger struct {
	directory string
	component string
}

var logLevel = 0

func write(level, message, directory, component string) {

	currentTime := time.Now()

	// Create the log file name
	fileName := fmt.Sprintf("./%s/%s-%d-%s.log", directory, currentTime.Format("2006-01-02"), currentTime.Hour(), component)

	file, err := os.OpenFile(fileName, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0644)

	if err != nil {

		return
	}

	defer file.Close()

	file.WriteString(fmt.Sprintf("%s %s %s\n", currentTime.Format("2006-01-02 15:04:05.999999999"), level, message))
}

func NewLogger(directory, component string) Logger {
	return Logger{
		directory: directory,
		component: component,
	}
}

func (l *Logger) Info(message string) {

	if logLevel <= Info {

		write("Info", message, l.directory, l.component)
	}
}
func (l *Logger) Error(message string) {

	write("Error", message, l.directory, l.component)

}
func (l *Logger) Debug(message string) {

	if logLevel <= Debug {

		write("Debug", message, l.directory, l.component)
	}
}
func (l *Logger) Trace(message string) {
	if logLevel <= Trace {

		write("Trace", message, l.directory, l.component)
	}
}
func (l *Logger) Fatal(message string) {

	write("Fatal", message, l.directory, l.component)

}
func (l *Logger) Warn(message string) {

	write("Warn", message, l.directory, l.component)

}

//func SetLogLevel(level int) {
//
//  logLevel = level
//}
