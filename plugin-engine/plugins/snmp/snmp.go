package snmp

import (
	"github.com/gosnmp/gosnmp"
	"plugin-engine/clients/snmpclient"
	"plugin-engine/utils"
)

const (
	ObjectIp      = "object.ip"
	SnmpCommunity = "snmp.community"
	SnmpPort      = "snmp.port"
	CredProfileId = "cred.profile.id"
)

var scalerOids = map[string]string{
	"system.name": ".1.3.6.1.2.1.1.5.0",
	//"system.description": ".1.3.6.1.2.1.1.1.0",
	//"system.objectId":    ".1.3.6.1.2.1.1.2.0",
	//"system.uptime":      ".1.3.6.1.2.1.1.3.0",
	//"system.interfaces":  ".1.3.6.1.2.1.2.1.0",
	//"system.location":    ".1.3.6.1.2.1.1.6.0",
}

var tabularOids = map[string]string{
	"interface.index":                 ".1.3.6.1.2.1.2.2.1.1",
	"interface.name":                  ".1.3.6.1.2.1.31.1.1.1.1",
	"interface.operational.status":    ".1.3.6.1.2.1.2.2.1.8",
	"interface.admin.status":          ".1.3.6.1.2.1.2.2.1.7",
	"interface.description":           ".1.3.6.1.2.1.2.2.1.2",
	"interface.sent.error.packet":     ".1.3.6.1.2.1.2.2.1.20",
	"interface.received.error.packet": ".1.3.6.1.2.1.2.2.1.14",
	"interface.sent.octets":           ".1.3.6.1.2.1.2.2.1.16",
	"interface.received.octets":       ".1.3.6.1.2.1.2.2.1.10",
	"interface.speed":                 ".1.3.6.1.2.1.2.2.1.5",
	"interface.alias":                 ".1.3.6.1.2.1.31.1.1.1.18",
	//"interface.physical.address":      ".1.3.6.1.2.1.2.2.1.6",
}

var discLogger = utils.NewLogger(utils.LogFilesPath, "discovery")

var collectLogger = utils.NewLogger(utils.LogFilesPath, "collect")

func Discovery(context map[string]interface{}, errors *[]map[string]interface{}) {

	validateContext(context)

	var client *gosnmp.GoSNMP

	if credentials, ok := context["credentials"].([]interface{}); ok {
		if !(len(credentials) > 0) {
			*errors = append(*errors, map[string]interface{}{

				utils.Error: "credentials not found",

				utils.ErrorCode: "SNMP01",

				utils.ErrorMsg: "error connecting to SNMP agent",
			})
			return
		}

	}

	for _, credential := range context["credentials"].([]interface{}) {

		client, _ = snmpclient.Init(context[ObjectIp].(string), credential.(map[string]interface{})[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)))

		defer snmpclient.Close(client)

		results, _ := snmpclient.Get(scalerOids, client)

		if len(results) > 0 {

			context[utils.Result] = results

			context[CredProfileId] = int(credential.(map[string]interface{})[CredProfileId].(float64))

			break
		}
	}

	return

}

func Collect(context map[string]interface{}, errors *[]map[string]interface{}) {

	validateContext(context)

	client, err := snmpclient.Init(context[ObjectIp].(string), context[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)))

	defer snmpclient.Close(client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.Error: err.Error(),

			utils.ErrorCode: "SNMP01",

			utils.ErrorMsg: "error connecting to SNMP agent",
		})

		collectLogger.Error("error connecting to SNMP agent: " + err.Error())

		return
	}

	results, err := snmpclient.Walk(tabularOids, client)

	if err != nil {
		*errors = append(*errors, map[string]interface{}{

			utils.Error: err.Error(),

			utils.ErrorCode: "SNMP02",

			utils.ErrorMsg: "error in collecting objects!",
		})

		discLogger.Error("error in discovery of device: " + err.Error())

		return
	}

	if len(results) > 0 {

		context[utils.Result] = results

	}

}

func validateContext(context map[string]interface{}) {

	if _, ok := context[ObjectIp]; !ok {
		context[ObjectIp] = "127.0.0.1"
	}

	//if _, ok := context[SnmpCommunity]; !ok {
	//	context[SnmpCommunity] = "public"
	//}

	if _, ok := context[SnmpPort]; !ok {
		context[SnmpPort] = 161
	}

}
