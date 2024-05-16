package snmpclient

import (
	"fmt"
	g "github.com/gosnmp/gosnmp"
	"net"
	"plugin-engine/utils"
	"strings"
	"time"
)

func Init(objectIp string, community string, port uint16, version g.SnmpVersion) (*g.GoSNMP, error) {

	GoSNMP := &g.GoSNMP{
		Target:    objectIp,
		Community: community,
		Port:      port,
		Version:   version,
		Retries:   3,
		Timeout:   time.Duration(3) * time.Second,
	}

	err := GoSNMP.Connect()

	if err != nil {

		return nil, fmt.Errorf("failed to connect to %s: %v", objectIp, err)

	}

	return GoSNMP, nil
}

func Close(GoSNMP *g.GoSNMP) error {

	if GoSNMP.Conn != nil {

		return GoSNMP.Conn.Close()

	}
	return nil
}

func Get(oidMap map[string]string, GoSNMP *g.GoSNMP) (map[string]interface{}, error) {

	resultMap := make(map[string]interface{})

	oids := make([]string, 0, len(oidMap))

	for _, oid := range oidMap {

		oids = append(oids, oid)

	}

	packet, err := GoSNMP.Get(oids)

	if err != nil {

		utils.DiscLogger.Error(err.Error())

		return nil, err
	}

	if packet.Error != 0 {

		utils.DiscLogger.Error(packet.Error.String())

		return nil, fmt.Errorf("SNMP error: %s", packet.Error)
	}

	if len(packet.Variables) != len(oidMap) {

		utils.DiscLogger.Error("unexpected number of SNMP variables returned")

		return nil, fmt.Errorf("unexpected number of SNMP variables returned")
	}

	for _, variable := range packet.Variables {

		for oidName, oid := range oidMap {

			if variable.Name == oid {

				switch variable.Type {

				case g.OctetString:

					resultMap[oidName] = string(variable.Value.([]byte))

				default:

					resultMap[oidName] = g.ToBigInt(variable.Value)

				}
			}

		}

	}
	return resultMap, nil
}

func Walk(oids map[string]string, GoSNMP *g.GoSNMP) ([]interface{}, error) {

	interfacesDetails := make([]interface{}, 0)

	results := map[string]map[string]interface{}{}

	for oidName, oid := range oids {

		err := GoSNMP.BulkWalk(oid, func(dataUnit g.SnmpPDU) error {

			tokens := strings.Split(dataUnit.Name, ".")

			interfaceIndex := tokens[len(tokens)-1]

			if _, ok := results[interfaceIndex]; !ok {

				results[interfaceIndex] = make(map[string]interface{})
			}

			if strings.EqualFold(oidName, utils.InterfacePhysicalAddress) {

				results[interfaceIndex][oidName] = resolveMacAddress(dataUnit.Value, dataUnit.Type)

			} else {

				results[interfaceIndex][oidName] = resolveValue(dataUnit.Value, dataUnit.Type)

			}

			return nil
		})

		if err != nil {

			utils.CollectLogger.Error(err.Error())

			return nil, err
		}

	}

	for _, interfaceData := range results {

		interfacesDetails = append(interfacesDetails, interfaceData)
	}

	utils.CollectLogger.Debug(interfacesDetails)

	return interfacesDetails, nil
}

func resolveValue(value interface{}, dataType g.Asn1BER) interface{} {

	if value == nil {

		return ""
	}

	switch dataType {

	case g.OctetString:

		return string(value.([]byte))

	case g.Integer:

		return g.ToBigInt(value)

	case g.Counter32, g.TimeTicks, g.Gauge32:

		return value.(uint)

	default:

		return value
	}
}

func resolveMacAddress(value interface{}, dataType g.Asn1BER) interface{} {

	if dataType == g.OctetString {

		if value != nil {

			return net.HardwareAddr(value.([]byte)).String()
		}
	}

	return ""
}
