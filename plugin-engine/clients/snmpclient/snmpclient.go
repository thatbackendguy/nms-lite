package snmpclient

import (
	"fmt"
	g "github.com/gosnmp/gosnmp"
	"time"
)

func Init(objectIp string, community string, port uint16) (*g.GoSNMP, error) {
	GoSNMP := &g.GoSNMP{
		Target:    objectIp,
		Community: community,
		Port:      port,
		Retries:   3,
		Timeout:   time.Duration(5) * time.Second,
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

func Get(oids []string, GoSNMP *g.GoSNMP) (*g.SnmpPacket, error) {
	return GoSNMP.Get(oids)
}

// TODO: implement Walk()
//func Walk()(){}
