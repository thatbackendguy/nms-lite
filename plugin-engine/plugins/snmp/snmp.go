package snmp

import (
	"github.com/gosnmp/gosnmp"
	"plugin-engine/clients/snmpclient"
	"plugin-engine/utils"
	"time"
)

const (
	ObjectIp      = "object.ip"
	Community     = "community"
	Port          = "port"
	Version       = "version"
	CredProfileId = "credential.profile.id"
	Credentials   = "credentials"
	Interface     = "interface"
	PollTime      = "poll.time"
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
	utils.InterfacePhysicalAddress:  ".1.3.6.1.2.1.2.2.1.6",
}

var logger = utils.NewLogger("plugins", "snmp")

func Discovery(context map[string]interface{}, errors *[]map[string]interface{}) {

	validateContext(context)

	var client *gosnmp.GoSNMP

	var err error

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

		client, err = snmpclient.Init(context[ObjectIp].(string), credential.(map[string]interface{})[Community].(string), uint16(context[Port].(float64)), credential.(map[string]interface{})[Version].(gosnmp.SnmpVersion))

		if err != nil {
			*errors = append(*errors, map[string]interface{}{

				utils.Error: "connection error",

				utils.ErrorCode: "SNMP01",

				utils.ErrorMsg: err.Error(),
			})

			return
		}

		defer snmpclient.Close(client)

		results, err := snmpclient.Get(scalerOids, client)

		if err != nil {
			*errors = append(*errors, map[string]interface{}{

				utils.Error: "invalid credentials/ connection timed out",

				utils.ErrorCode: "SNMP01",

				utils.ErrorMsg: err.Error(),
			})
		}

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

	client, err := snmpclient.Init(context[ObjectIp].(string), context[Community].(string), uint16(context[Port].(float64)), context[Version].(gosnmp.SnmpVersion))

	defer snmpclient.Close(client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.Error: err.Error(),

			utils.ErrorCode: "SNMP01",

			utils.ErrorMsg: "error connecting to SNMP agent",
		})

		utils.CollectLogger.Error("error connecting to SNMP agent: " + err.Error())

		return
	}

	results, err := snmpclient.Walk(tabularOids, client)

	if err != nil {

		*errors = append(*errors, map[string]interface{}{

			utils.Error: err.Error(),

			utils.ErrorCode: "SNMP02",

			utils.ErrorMsg: "error in collecting objects!",
		})

		utils.DiscLogger.Error("error in discovery of device: " + err.Error())

		return
	}

	if len(results) > 0 {

		context[utils.Result] = map[string]interface{}{

			Interface: results,
		}

		context[PollTime] = time.Now().Format("2006-01-02 15:04:05")

	}

}

func validateContext(context map[string]interface{}) {

	var isMultiContext bool

	// for multiple context
	if credentials, ok := context[Credentials]; ok {

		isMultiContext = true

		for _, credential := range credentials.([]interface{}) {

			if value, ok := credential.(map[string]interface{})[Version]; ok {

				switch value {

				case "v1":

					credential.(map[string]interface{})[Version] = gosnmp.Version1

				case "v3":

					credential.(map[string]interface{})[Version] = gosnmp.Version3

				default:

					credential.(map[string]interface{})[Version] = gosnmp.Version2c

				}

			} else {

				credential.(map[string]interface{})[Version] = gosnmp.Version2c
			}
		}
	}

	if _, ok := context[ObjectIp]; !ok {

		context[ObjectIp] = "127.0.0.1"
	}

	if _, ok := context[Port]; !ok {

		context[Port] = 161
	}

	// for single device context
	if !isMultiContext {

		if value, ok := context[Version]; ok {

			switch value {

			case "v1":

				context[Version] = gosnmp.Version1

			case "v3":

				context[Version] = gosnmp.Version3

			default:

				context[Version] = gosnmp.Version2c
			}

		} else {

			context[Version] = gosnmp.Version2c
		}
	}

}
