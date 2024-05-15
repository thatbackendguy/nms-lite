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
	SnmpVersion   = "version"
	CredProfileId = "cred.profile.id"
	Credentials   = "credentials"
	Interface     = "interface"
)

const (
	SystemName        = "system.name"
	SystemDescription = "system.description"
	SystemObjectId    = "system.objectId"
	SystemUptime      = "system.uptime"
	SystemInterfaces  = "system.interfaces"
	SystemLocation    = "system.location"
)

const (
	InterfaceIndexKey               = "interface.index"
	InterfaceNameKey                = "interface.name"
	InterfaceOperationalStatusKey   = "interface.operational.status"
	InterfaceAdminStatusKey         = "interface.admin.status"
	InterfaceDescriptionKey         = "interface.description"
	InterfaceSentErrorPacketKey     = "interface.sent.error.packet"
	InterfaceReceivedErrorPacketKey = "interface.received.error.packet"
	InterfaceSentOctetsKey          = "interface.sent.octets"
	InterfaceReceivedOctetsKey      = "interface.received.octets"
	InterfaceSpeedKey               = "interface.speed"
	InterfaceAliasKey               = "interface.alias"
	InterfacePhysicalAddress        = "interface.physical.address"
)

var scalerOids = map[string]string{
	SystemName:        ".1.3.6.1.2.1.1.5.0",
	SystemDescription: ".1.3.6.1.2.1.1.1.0",
	SystemObjectId:    ".1.3.6.1.2.1.1.2.0",
	SystemUptime:      ".1.3.6.1.2.1.1.3.0",
	SystemInterfaces:  ".1.3.6.1.2.1.2.1.0",
	SystemLocation:    ".1.3.6.1.2.1.1.6.0",
}

var tabularOids = map[string]string{
	InterfaceIndexKey:               ".1.3.6.1.2.1.2.2.1.1",
	InterfaceNameKey:                ".1.3.6.1.2.1.31.1.1.1.1",
	InterfaceOperationalStatusKey:   ".1.3.6.1.2.1.2.2.1.8",
	InterfaceAdminStatusKey:         ".1.3.6.1.2.1.2.2.1.7",
	InterfaceDescriptionKey:         ".1.3.6.1.2.1.2.2.1.2",
	InterfaceSentErrorPacketKey:     ".1.3.6.1.2.1.2.2.1.20",
	InterfaceReceivedErrorPacketKey: ".1.3.6.1.2.1.2.2.1.14",
	InterfaceSentOctetsKey:          ".1.3.6.1.2.1.2.2.1.16",
	InterfaceReceivedOctetsKey:      ".1.3.6.1.2.1.2.2.1.10",
	InterfaceSpeedKey:               ".1.3.6.1.2.1.2.2.1.5",
	InterfaceAliasKey:               ".1.3.6.1.2.1.31.1.1.1.18",
	//InterfacePhysicalAddress:        ".1.3.6.1.2.1.2.2.1.6",
}

var discLogger = utils.NewLogger(utils.LogFilesPath, utils.DiscLoggerName)

var collectLogger = utils.NewLogger(utils.LogFilesPath, utils.CollectLoggerName)

func Discovery(context map[string]interface{}, errors *[]map[string]interface{}) {

	validateContext(context)

	var client *gosnmp.GoSNMP

	if credentials, ok := context[Credentials].([]interface{}); ok {
		if !(len(credentials) > 0) {
			*errors = append(*errors, map[string]interface{}{

				utils.Error: "credentials not found",

				utils.ErrorCode: "SNMP01",

				utils.ErrorMsg: "error connecting to SNMP agent",
			})
			return
		}

	}

	for _, credential := range context[Credentials].([]interface{}) {

		client, _ = snmpclient.Init(context[ObjectIp].(string), credential.(map[string]interface{})[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)), credential.(map[string]interface{})[SnmpVersion].(gosnmp.SnmpVersion))

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

	client, err := snmpclient.Init(context[ObjectIp].(string), context[SnmpCommunity].(string), uint16(context[SnmpPort].(float64)), context[SnmpVersion].(gosnmp.SnmpVersion))

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

		context[utils.Result] = map[string]interface{}{
			Interface: results,
		}

	}

}

func validateContext(context map[string]interface{}) {

	// for multiple context
	if credentials, ok := context[Credentials]; ok {
		for _, credential := range credentials.([]interface{}) {
			if value, ok := credential.(map[string]interface{})[SnmpVersion]; ok {
				switch value {
				case "v1":
					credential.(map[string]interface{})[SnmpVersion] = gosnmp.Version1
				case "v3":
					credential.(map[string]interface{})[SnmpVersion] = gosnmp.Version3
				default:
					credential.(map[string]interface{})[SnmpVersion] = gosnmp.Version2c
				}
			} else {
				credential.(map[string]interface{})[SnmpVersion] = gosnmp.Version2c
			}
		}
	}

	if _, ok := context[ObjectIp]; !ok {
		context[ObjectIp] = "127.0.0.1"
	}

	if _, ok := context[SnmpPort]; !ok {
		context[SnmpPort] = 161
	}

	// for single device context
	if value, ok := context[SnmpVersion]; ok {
		switch value {
		case "v1":
			context[SnmpVersion] = gosnmp.Version1
		case "v3":
			context[SnmpVersion] = gosnmp.Version3
		default:
			context[SnmpVersion] = gosnmp.Version2c
		}
	} else {
		context[SnmpVersion] = gosnmp.Version2c
	}
}
