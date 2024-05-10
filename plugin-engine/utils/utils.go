package utils

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"log"
	"os"
	"path/filepath"
	"time"
)

var Loggers = make(map[string]*zap.Logger)

func SendResponse(context []map[string]interface{}) {

	jsonOutput, err := json.Marshal(context)

	if err != nil {

		log.Fatal("JSON Marshal Error: ", err)

	}

	encodedString := base64.StdEncoding.EncodeToString(jsonOutput)

	Loggers["pluginEngine"].Info(encodedString)

	fmt.Println(encodedString)
}

func LoggerInit() {
	logFileNames := map[string]string{
		"discovery":    "discovery",
		"collect":      "collect",
		"pluginEngine": "pluginEngine",
	}

	cfg := zap.NewProductionConfig()

	cfg.EncoderConfig.EncodeTime = zapcore.TimeEncoderOfLayout("2006-01-02 15:04:05.000")

	logDir := "/home/yash/Documents/GitHub/nms-lite/plugin-engine/logs"

	_ = os.MkdirAll(logDir, 0755)

	syncers := make(map[string]zapcore.WriteSyncer)

	for eventType, fileName := range logFileNames {

		logFilePath := filepath.Join(logDir, generateLogFileName(fileName))

		file, _ := os.OpenFile(logFilePath, os.O_WRONLY|os.O_APPEND|os.O_CREATE, 0644)

		syncers[eventType] = zapcore.AddSync(file)
	}

	for eventType, syncer := range syncers {

		core := zapcore.NewCore(
			zapcore.NewConsoleEncoder(cfg.EncoderConfig),
			syncer,
			zapcore.DebugLevel,
		)

		Loggers[eventType] = zap.New(core, zap.AddCaller())
	}

	for _, loggerName := range logFileNames {
		Loggers[loggerName].Info(loggerName + " Logger Init Success")
	}
}

func generateLogFileName(baseName string) string {
	now := time.Now()
	year, month, day := now.Date()
	hour := now.Hour()
	return fmt.Sprintf("%s-%02d-%02d-%d-%02d-00-00.log", baseName, day, month, year, hour)
}
