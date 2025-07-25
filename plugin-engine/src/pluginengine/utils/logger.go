package utils

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
)

const (
	Info  = 2
	Debug = 1
	Trace = 0
)

type Logger struct {
	directory, component string
}

var logLevel = 0

func write(level, directory, component string, message string) {

	currentTime := time.Now()

	// Create the log file name
	fileName := fmt.Sprintf("./%s/%s-%d-%s.log", directory, currentTime.Format("2006-01-02"), currentTime.Hour(), component)

	file, err := os.OpenFile(fileName, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0644)

	if err != nil {
		return
	}

	defer file.Close()

	_, err = file.WriteString(fmt.Sprintf("%s %s %s\n", currentTime.Format("2006-01-02 15:04:05.999999999"), level, message))

	if err != nil {
		return
	}
}

func GetLogger(directory, component string) Logger {

	dirPath := filepath.Join(".", directory)

	_ = os.MkdirAll(dirPath, 0755)

	return Logger{
		directory: directory,
		component: component,
	}
}

func (l *Logger) Info(message string) {

	if logLevel <= Info {

		write("Info", l.directory, l.component, message)
	}
}
func (l *Logger) Error(message string) {

	write("Error", l.directory, l.component, message)

}
func (l *Logger) Debug(message string) {

	if logLevel <= Debug {

		write("Debug", l.directory, l.component, message)
	}
}
func (l *Logger) Trace(message string) {
	if logLevel <= Trace {

		write("Trace", l.directory, l.component, message)
	}
}
func (l *Logger) Fatal(message string) {

	write("Fatal", l.directory, l.component, message)

}
func (l *Logger) Warn(message string) {

	write("Warn", l.directory, l.component, message)

}

//func SetLogLevel(level int) {
//
//  logLevel = level
//}
