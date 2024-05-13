package snmpclient

import (
	"fmt"
	g "github.com/gosnmp/gosnmp"
	"sync"
	"time"
)

func Init(objectIp string, community string, port uint16) (*g.GoSNMP, error) {

	GoSNMP := &g.GoSNMP{
		Target:    objectIp,
		Community: community,
		Port:      port,
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

		return nil, err

	}

	if packet.Error != 0 {

		return nil, fmt.Errorf("SNMP error: %s", packet.Error)

	}

	if len(packet.Variables) != len(oidMap) {

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

func Walk(oidMap map[string]string, GoSNMP *g.GoSNMP) (map[string]interface{}, error) {

	var wg sync.WaitGroup

	resultMap := make(map[string]interface{})

	for oidName, oid := range oidMap {

		oidResult := make(map[string]interface{})

		wg.Add(1)

		results, _ := GoSNMP.WalkAll(oid)

		go func(results []g.SnmpPDU, oidName string) {

			defer wg.Done()

			for _, result := range results {

				switch result.Type {
				case g.OctetString:
					oidResult[result.Name] = string(result.Value.([]byte))
				case g.Integer:
					oidResult[result.Name] = g.ToBigInt(result.Value)
				case g.Counter32:
					oidResult[result.Name] = result.Value.(uint)
				case g.Gauge32:
					oidResult[result.Name] = result.Value.(uint)
				case g.TimeTicks:
					oidResult[result.Name] = result.Value.(uint)
				default:
					oidResult[result.Name] = result.Value
				}
			}

			resultMap[oidName] = oidResult

		}(results, oidName)
	}

	wg.Wait()

	return resultMap, nil
}
