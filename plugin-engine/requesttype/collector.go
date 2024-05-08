package requesttype

import (
	"github.com/gosnmp/gosnmp"
	"log"
	"plugin-engine/constants"
	"plugin-engine/utils"
)

func Collect(context map[string]interface{}) {

	resultMap := make(map[string]interface{})

	gosnmp.Default.Target = context[constants.OBJECT_IP].(string)

	gosnmp.Default.Community = context[constants.COMMUNITY].(string)

	gosnmp.Default.Port = uint16(context[constants.SNMP_PORT].(float64))

	err := gosnmp.Default.Connect()

	if err != nil {
		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "SNMP01"

		errMap[constants.ERROR_MSG] = "Error connecting to SNMP agent"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Println("Error connecting to SNMP agent:", err)

		return
	}

	defer gosnmp.Default.Conn.Close()

	// TODO: Collect Metrics here (Tabular OIDs)

	context[constants.RESULT] = resultMap

	context[constants.STATUS] = constants.SUCCESS
}
