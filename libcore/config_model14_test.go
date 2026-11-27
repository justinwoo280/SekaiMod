package libcore

import (
	"encoding/json"
	"os"
	"testing"
)

func TestGeneratedConfigModel14Configurations(t *testing.T) {
	path := os.Getenv("SEKAIMOD_CONFIG14_FIXTURE")
	if path == "" {
		t.Skip("no Kotlin-generated configuration-model fixture supplied")
	}
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	var fixtures map[string]struct {
		Config json.RawMessage `json:"config"`
		Valid  bool            `json:"valid"`
	}
	if err := json.Unmarshal(data, &fixtures); err != nil {
		t.Fatal(err)
	}
	if len(fixtures) == 0 {
		t.Fatal("empty configuration-model fixture")
	}
	for name, fixture := range fixtures {
		t.Run(name, func(t *testing.T) {
			err := CheckSingBoxConfig(string(fixture.Config), nil)
			if fixture.Valid && err != nil {
				t.Fatal(err)
			}
			if !fixture.Valid && err == nil {
				t.Fatal("invalid configuration was accepted")
			}
		})
	}
}
