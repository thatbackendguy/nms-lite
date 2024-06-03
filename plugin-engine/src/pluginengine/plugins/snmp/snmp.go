package snmp

import (
	"github.com/gosnmp/gosnmp"
	snmp "plugin-engine/src/pluginengine/clients/snmpclient"
	"plugin-engine/src/pluginengine/consts"
	"plugin-engine/src/pluginengine/utils"
	"time"
)

var scalarOids = map[string]string{
	consts.SystemName:        ".1.3.6.1.2.1.1.5.0",
	consts.SystemDescription: ".1.3.6.1.2.1.1.1.0",
	consts.SystemObjectId:    ".1.3.6.1.2.1.1.2.0",
	consts.SystemUptime:      ".1.3.6.1.2.1.1.3.0",
	consts.SystemInterfaces:  ".1.3.6.1.2.1.2.1.0",
	consts.SystemLocation:    ".1.3.6.1.2.1.1.6.0",
}

var tabularOids = map[string]string{
	consts.InterfaceIndexKey:               ".1.3.6.1.2.1.2.2.1.1",
	consts.InterfaceNameKey:                ".1.3.6.1.2.1.31.1.1.1.1",
	consts.InterfaceOperationalStatusKey:   ".1.3.6.1.2.1.2.2.1.8",
	consts.InterfaceAdminStatusKey:         ".1.3.6.1.2.1.2.2.1.7",
	consts.InterfaceDescriptionKey:         ".1.3.6.1.2.1.2.2.1.2",
	consts.InterfaceSentErrorPacketKey:     ".1.3.6.1.2.1.2.2.1.20",
	consts.InterfaceReceivedErrorPacketKey: ".1.3.6.1.2.1.2.2.1.14",
	consts.InterfaceSentOctetsKey:          ".1.3.6.1.2.1.2.2.1.16",
	consts.InterfaceReceivedOctetsKey:      ".1.3.6.1.2.1.2.2.1.10",
	consts.InterfaceSpeedKey:               ".1.3.6.1.2.1.2.2.1.5",
	consts.InterfaceAliasKey:               ".1.3.6.1.2.1.31.1.1.1.18",
	consts.InterfacePhysicalAddress:        ".1.3.6.1.2.1.2.2.1.6",
}

var logger = utils.GetLogger(consts.LogFilesPath+"/plugins", "snmp")

func Discovery(context map[string]interface{}) {

	errors := make([]map[string]string, 0)

	validateContext(context)

	var client *gosnmp.GoSNMP

	var err error

	if credentials, ok := context[consts.Credentials].([]interface{}); ok {

		if !(len(credentials) > 0) {

			errors = append(errors, map[string]string{

				consts.Error: "credentials not found",

				consts.ErrorCode: "SNMP01",

				consts.ErrorMsg: "error connecting to SNMP agent",
			})

			logger.Error("error connecting to SNMP agent. reason: credentials not found")

			return
		}

	}

	for _, credential := range context[consts.Credentials].([]interface{}) {

		client, err = snmp.Init(context[consts.ObjectIp].(string), credential.(map[string]interface{})[consts.Community].(string), uint16(context[consts.Port].(float64)), credential.(map[string]interface{})[consts.Version].(gosnmp.SnmpVersion))

		if err != nil {
			errors = append(errors, map[string]string{

				consts.Error: "connection error",

				consts.ErrorCode: "SNMP01",

				consts.ErrorMsg: err.Error(),
			})

			logger.Error("error connecting from " + context[consts.ObjectIp].(string) + " to SNMP agent. reason: " + err.Error())

			return
		}

		defer snmp.Close(client)

		results, err := snmp.Get(scalarOids, client)

		if err != nil {
			errors = append(errors, map[string]string{

				consts.Error: "invalid credentials/ connection timed out",

				consts.ErrorCode: "SNMP01",

				consts.ErrorMsg: err.Error(),
			})

			logger.Error("error connecting from " + context[consts.ObjectIp].(string) + " to SNMP agent. reason: " + err.Error())
		}

		if len(results) > 0 {

			context[consts.Result] = results

			context[consts.CredProfileId] = int(credential.(map[string]interface{})[consts.CredProfileId].(float64))

			break
		}
	}

	context[consts.Error] = errors

	if _, ok := context[consts.Result]; ok {

		if len(context[consts.Result].(map[string]interface{})) <= 0 && len(errors) > 0 {

			context[consts.Status] = consts.Failed

		} else {

			context[consts.Status] = consts.Success

		}

	} else {

		context[consts.Status] = consts.Failed

	}

	return

}

func Collect(context map[string]interface{}) {

	errors := make([]map[string]string, 0)

	validateContext(context)

	client, err := snmp.Init(context[consts.ObjectIp].(string), context[consts.Community].(string), uint16(context[consts.Port].(float64)), context[consts.Version].(gosnmp.SnmpVersion))

	defer snmp.Close(client)

	if err != nil {

		errors = append(errors, map[string]string{

			consts.Error: err.Error(),

			consts.ErrorCode: "SNMP01",

			consts.ErrorMsg: "error connecting to SNMP agent",
		})

		logger.Error("error connecting to SNMP agent: " + err.Error())

		return
	}

	results, err := snmp.Walk(tabularOids, client)

	if err != nil {

		errors = append(errors, map[string]string{

			consts.Error: err.Error(),

			consts.ErrorCode: "SNMP02",

			consts.ErrorMsg: "error in collecting objects!",
		})

		logger.Error("error in discovery of device: " + err.Error())

		return
	}

	if len(results) > 0 {

		context[consts.Result] = map[string]interface{}{

			consts.Interface: results,
		}

		context[consts.PollTime] = time.Now().Format("2006-01-02 15:04:05")

	}

	context[consts.Error] = errors

	if _, ok := context[consts.Result]; ok {

		if len(context[consts.Result].(map[string]interface{})) <= 0 && len(errors) > 0 {

			context[consts.Status] = consts.Failed

		} else {

			context[consts.Status] = consts.Success

		}

	} else {

		context[consts.Status] = consts.Failed

	}

}

func validateContext(context map[string]interface{}) {

	if credentials, ok := context[consts.Credentials]; ok {

		for _, credential := range credentials.([]interface{}) {

			if value, ok := credential.(map[string]interface{})[consts.Version]; ok {

				switch value {

				case "v1":

					credential.(map[string]interface{})[consts.Version] = gosnmp.Version1

				case "v3":

					credential.(map[string]interface{})[consts.Version] = gosnmp.Version3

				default:

					credential.(map[string]interface{})[consts.Version] = gosnmp.Version2c

				}

			} else {

				credential.(map[string]interface{})[consts.Version] = gosnmp.Version2c
			}
		}
	} else {
		if value, ok := context[consts.Version]; ok {

			switch value {

			case "v1":

				context[consts.Version] = gosnmp.Version1

			case "v3":

				context[consts.Version] = gosnmp.Version3

			default:

				context[consts.Version] = gosnmp.Version2c
			}

		} else {

			context[consts.Version] = gosnmp.Version2c
		}
	}

	if _, ok := context[consts.ObjectIp]; !ok {

		context[consts.ObjectIp] = consts.Localhost
	}

	if _, ok := context[consts.Port]; !ok {

		context[consts.Port] = consts.DefaultSnmpPort
	}
}
