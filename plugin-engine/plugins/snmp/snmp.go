package snmp

import (
	"fmt"
	"plugin-engine/clients/snmpclient"
	"plugin-engine/utils"
)

const (
	ObjectIp          = "object.ip"
	SnmpCommunity     = "snmp.community"
	SnmpPort          = "snmp.port"
	SystemName        = "system.name"
	SystemDescription = "system.description"
	SystemLocation    = "system.location"
	SystemObjectId    = "system.objectId"
	SystemUptime      = "system.uptime"
	SystemInterfaces  = "system.interfaces"
)

func Discovery(context map[string]interface{}, errors *[]map[string]interface{}) {

	results := make(map[string]interface{})

	validateContext(context, "discovery")

	client, err := snmpclient.Init(context[ObjectIp].(string), context[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)))

	defer snmpclient.Close(client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.Error: err.Error(),

			utils.ErrorCode: "SNMP01",

			utils.ErrorMsg: "Error connecting to SNMP agent",
		})

		utils.Loggers["discovery"].Error("Error connecting to SNMP agent:" + err.Error())

		return
	}

	// Get system name
	sysName, err := snmpclient.Get([]string{"1.3.6.1.2.1.1.5.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system name",

			utils.ErrorCode: "DISC01",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system name:" + err.Error())

	} else {
		results[SystemName] = string(sysName.Variables[0].Value.([]uint8))
	}

	// Get system description

	sysDesc, err := snmpclient.Get([]string{"1.3.6.1.2.1.1.1.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system description",

			utils.ErrorCode: "DISC02",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system description:" + err.Error())

	} else {
		results[SystemDescription] = fmt.Sprintf("%s", sysDesc.Variables[0].Value)
	}

	// Get system location

	sysLoc, err := snmpclient.Get([]string{"1.3.6.1.2.1.1.6.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system location",

			utils.ErrorCode: "DISC03",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system location:" + err.Error())
	} else {

		results[SystemLocation] = fmt.Sprintf("%s", sysLoc.Variables[0].Value)
	}

	// Get system objectId

	sysObjId, err := snmpclient.Get([]string{"1.3.6.1.2.1.1.2.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system objectId",

			utils.ErrorCode: "DISC04",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system objectId:" + err.Error())

	} else {

		results[SystemObjectId] = fmt.Sprintf("%s", sysObjId.Variables[0].Value)
	}

	// Get system uptime

	sysUptime, err := snmpclient.Get([]string{"1.3.6.1.2.1.1.3.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system uptime",

			utils.ErrorCode: "DISC05",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system uptime:" + err.Error())

	} else {

		results[SystemUptime] = sysUptime.Variables[0].Value.(uint32)
	}

	// Get system interfaces

	sysInterfaces, err := snmpclient.Get([]string{"1.3.6.1.2.1.2.1.0"}, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.ErrorMsg: "Error getting system interfaces",

			utils.ErrorCode: "DISC06",

			utils.Error: err.Error(),
		})

		utils.Loggers["discovery"].Error("Error getting system interfaces:" + err.Error())

	} else {
		results[SystemInterfaces] = sysInterfaces.Variables[0].Value.(int)
	}

	if len(results) > 0 {
		context[utils.Result] = results
	}

}

func Collect(context map[string]interface{}, errors *[]map[string]interface{}) {

	//resultMap := make(map[string]interface{})
	//
	//validateContext(context, "collect")
	//
	//client := snmpclient.Init(context[ObjectIp].(string), context[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)))
	//
	//err := snmpclient.Connect(client)
	//
	//if err != nil {
	//
	//	*errors = append(*errors, map[string]interface{}{
	//
	//		utils.ErrorMsg: "Error connecting to SNMP agent",
	//
	//		utils.ErrorCode: "SNMP01",
	//
	//		utils.Error: err.Error(),
	//	})
	//
	//	utils.Loggers["collect"].Error("Error connecting to SNMP agent:" + err.Error())
	//
	//	return
	//}
	//
	//defer snmpclient.Close(client)
	//
	//// TODO: Collect Metrics here (Tabular OIDs)
	//
	//context[utils.Result] = resultMap

}

func validateContext(context map[string]interface{}, loggerName string) {

	if _, ok := context[ObjectIp]; !ok {

		utils.Loggers[loggerName].Warn("context['object.ip'] not found, setting default value: 127.0.0.1")

		context[ObjectIp] = "127.0.0.1"

	}

	if _, ok := context[SnmpCommunity]; !ok {

		utils.Loggers[loggerName].Warn("context['snmp.community'] not found, setting default value: 'public'")

		context[SnmpCommunity] = "public"
	}

	if _, ok := context[SnmpPort]; !ok {

		utils.Loggers[loggerName].Warn("context['snmp.port'] not found, setting default value: 161")

		context[SnmpPort] = 161
	}

}
