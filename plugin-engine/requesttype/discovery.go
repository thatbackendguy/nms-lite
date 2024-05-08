package requesttype

import (
	"fmt"
	"github.com/gosnmp/gosnmp"
	"log"
	"plugin-engine/constants"
	"plugin-engine/utils"
)

func Discovery(context map[string]interface{}) {

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

		log.Fatal("Error connecting to SNMP agent:", err)

		return
	}

	defer gosnmp.Default.Conn.Close()

	// Get system name
	sysName, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.1.5.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC01"

		errMap[constants.ERROR_MSG] = "Error getting system name"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system name:", err)

		return

	}

	resultMap[constants.SYSTEM_NAME] = string(sysName.Variables[0].Value.([]uint8))

	// Get system description

	sysDesc, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.1.1.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC02"

		errMap[constants.ERROR_MSG] = "Error getting system description"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system description:", err)

		return

	}

	resultMap[constants.SYSTEM_DESCRIPTION] = fmt.Sprintf("%s", sysDesc.Variables[0].Value)

	// Get system location

	sysLoc, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.1.6.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC03"

		errMap[constants.ERROR_MSG] = "Error getting system location"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system location:", err)

		return

	}

	resultMap[constants.SYSTEM_LOCATION] = fmt.Sprintf("%s", sysLoc.Variables[0].Value)

	// Get system objectId

	sysObjId, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.1.2.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC04"

		errMap[constants.ERROR_MSG] = "Error getting system objectId"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system objectId:", err)

		return

	}

	resultMap[constants.SYSTEM_OBJECT_ID] = fmt.Sprintf("%s", sysObjId.Variables[0].Value)

	// Get system uptime

	sysUptime, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.1.3.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC05"

		errMap[constants.ERROR_MSG] = "Error getting system uptime"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system uptime:", err)

		return

	}

	resultMap[constants.SYSTEM_UPTIME] = sysUptime.Variables[0].Value.(uint32)

	// Get system interfaces

	sysInterfaces, err := gosnmp.Default.Get([]string{"1.3.6.1.2.1.2.1.0"})

	if err != nil {

		context[constants.STATUS] = constants.FAILED

		errMap := make(map[string]interface{})

		errMap[constants.ERROR] = err.Error()

		errMap[constants.ERROR_CODE] = "DISC06"

		errMap[constants.ERROR_MSG] = "Error getting system interfaces"

		context[constants.ERROR] = errMap

		utils.SendResponse(context)

		log.Fatal("Error getting system interfaces:", err)

		return

	}

	resultMap[constants.SYSTEM_INTERFACES] = sysInterfaces.Variables[0].Value.(int)

	context[constants.RESULT] = resultMap

	context[constants.STATUS] = constants.SUCCESS
}
